/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okio;

import static okio.Util.checkOffsetAndCount;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.wos.log.ILogger;
import com.wos.log.LoggerBuilder;

/**
 * This timeout uses a background thread to take action exactly when the timeout
 * occurs. Use this to implement timeouts where they aren't supported natively,
 * such as to sockets that are blocked on writing.
 *
 * <p>
 * Subclasses should override {@link #timedOut} to take action when a timeout
 * occurs. This method will be invoked by the shared watchdog thread so it
 * should not do any long-running operations. Otherwise we risk starving other
 * timeouts from being triggered.
 *
 * <p>
 * Use {@link #sink} and {@link #source} to apply this timeout to a stream. The
 * returned value will apply the timeout to each operation on the wrapped
 * stream.
 *
 * <p>
 * Callers should call {@link #enter} before doing work that is subject to
 * timeouts, and {@link #exit} afterwards. The return value of {@link #exit}
 * indicates whether a timeout was triggered. Note that the call to
 * {@link #timedOut} is asynchronous, and may be called after {@link #exit}.
 */
public class AsyncTimeout extends Timeout {
	private static final ILogger ILOG = LoggerBuilder.getLogger(AsyncTimeout.class);
	/**
	 * Don't write more than 64 KiB of data at a time, give or take a segment.
	 * Otherwise slow connections may suffer timeouts even when they're making
	 * (slow) progress. Without this, writing a single 1 MiB buffer may never
	 * succeed on a sufficiently slow connection.
	 */
	private static final int TIMEOUT_WRITE_SIZE = 64 * 1024;

	/**
	 * Duration for the watchdog thread to be idle before it shuts itself down.
	 */
	private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60);
	private static final long IDLE_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(IDLE_TIMEOUT_MILLIS);

	/**
	 * The watchdog thread processes a linked list of pending timeouts, sorted
	 * in the order to be triggered. This class synchronizes on
	 * AsyncTimeout.class. This lock guards the queue.
	 *
	 * <p>
	 * Head's 'next' points to the first element of the linked list. The first
	 * element is the next node to time out, or null if the queue is empty. The
	 * head is null until the watchdog thread is started and also after being
	 * idle for {@link #IDLE_TIMEOUT_MILLIS}.
	 */
	// static AsyncTimeout head;

	/** True if this node is currently in the queue. */
	private boolean inQueue;

	/** The next node in the linked list. */
	private AsyncTimeout next;

	/**
	 * If scheduled, this is the time that the watchdog should time this out.
	 */
	private long timeoutAt;

	
	/** /////////////////////////////////////////////////////////////////////////// **/
	private static final int LOCK_SIZE = Runtime.getRuntime().availableProcessors();
	
	private static final Random RANDOM = new Random(System.currentTimeMillis());
	
	private final Locker locker;
	
	private int id;
	
	public AsyncTimeout() {
		this.id = RANDOM.nextInt(LOCK_SIZE);
		this.locker = LOCKERS.get(this.id);
		if(ILOG.isDebugEnabled()) {
			ILOG.debug("id is : " + this.id + "; and locker is : " + this.locker);
		}
	}
	
	private Locker getLocker() {
		return this.locker;
	}
	
	private static final Map<Integer, Locker> LOCKERS = new HashMap<Integer, Locker>();
	static {
		for(int i=0; i<LOCK_SIZE; i++) {
			LOCKERS.put(i, new Locker(i));
		}
	}
	
	private static class Locker {

		private int lockerId;

		public AsyncTimeout head;

		public Locker(int id) {
			this.lockerId = id;
		}

		@Override
		public String toString() {
			return "Locker [lockerId=" + lockerId + "]";
		}
		
		public void startWatch() {
			if (head == null) {
				head = new AsyncTimeout();
				new Watchdog(this).start();
			}
		}
	}

	/** /////////////////////////////////////////////////////////////////////////// **/
	
	
	public final void enter() {
		if (inQueue)
			throw new IllegalStateException("Unbalanced enter/exit");
		long timeoutNanos = timeoutNanos();
		boolean hasDeadline = hasDeadline();
		if (timeoutNanos == 0 && !hasDeadline) {
			return; // No timeout and no deadline? Don't bother with the queue.
		}
		inQueue = true;
		scheduleTimeout(this, timeoutNanos, hasDeadline);
	}

	private static void scheduleTimeout(AsyncTimeout node, long timeoutNanos, boolean hasDeadline) {
		synchronized (node.getLocker()) {
			long start = System.nanoTime();
			node.getLocker().startWatch();
			// Start the watchdog thread and create the head node when the first
			// timeout is scheduled.
//			if (head == null) {
//				head = new AsyncTimeout();
//				new Watchdog().start();
//			}

			long now = System.nanoTime();
			if (timeoutNanos != 0 && hasDeadline) {
				// Compute the earliest event; either timeout or deadline. Because
				// nanoTime can wrap around,
				// Math.min() is undefined for absolute values, but meaningful for
				// relative ones.
				node.timeoutAt = now + Math.min(timeoutNanos, node.deadlineNanoTime() - now);
			} else if (timeoutNanos != 0) {
				node.timeoutAt = now + timeoutNanos;
			} else if (hasDeadline) {
				node.timeoutAt = node.deadlineNanoTime();
			} else {
				throw new AssertionError();
			}

			// Insert the node in sorted order.
			long remainingNanos = node.remainingNanos(now);
			for (AsyncTimeout prev = node.getLocker().head; true; prev = prev.next) {
				if (prev.next == null || remainingNanos < prev.next.remainingNanos(now)) {
					node.next = prev.next;
					prev.next = node;
					if (prev == node.getLocker().head) {
					    // fix findbugs NO_NOTIFY_NOT_NOTIFYALL, change notify() to notifyAll()
						node.getLocker().notifyAll(); // Wake up the watchdog when
														// inserting at the front.
					}
					break;
				}
			}
			
			if(ILOG.isDebugEnabled()) {
				ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
						+ " - Method: scheduleTimeout.in()");
			}
			
		}
	}

	/**
	 * @return Returns true if the timeout occurred.
	 */
	public final boolean exit() {
		if (!inQueue)
			return false;
		inQueue = false;
		return cancelScheduledTimeout(this);
	}

	/** Returns true if the timeout occurred. */
	private static boolean cancelScheduledTimeout(AsyncTimeout node) {
		// Remove the node from the linked list.
		synchronized(node.getLocker()){
			long start = System.nanoTime();
			for (AsyncTimeout prev = node.getLocker().head; prev != null; prev = prev.next) {
				if (prev.next == node) {
					prev.next = node.next;
					node.next = null;

					if(ILOG.isDebugEnabled()) {
						ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
								+ " - Method: cancelScheduledTimeout.in.1()");
					}
					return false;
				}
			}

			// The node wasn't found in the linked list: it must have timed out!

			if(ILOG.isDebugEnabled()) {
				ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
						+ " - Method: cancelScheduledTimeout.in.2()");
			}
			return true;
		}
	}

	/**
	 * Returns the amount of time left until the time out. This will be negative
	 * if the timeout has elapsed and the timeout should occur immediately.
	 */
	private long remainingNanos(long now) {
		return timeoutAt - now;
	}

	/**
	 * Invoked by the watchdog thread when the time between calls to
	 * {@link #enter()} and {@link #exit()} has exceeded the timeout.
	 */
	protected void timedOut() {
	}

	/**
	 *
	 * Returns a new sink that delegates to {@code sink}, using this to
	 * implement timeouts. This works best if {@link #timedOut} is overridden to
	 * interrupt {@code sink}'s current operation.
	 * @param sink Sink
	 * @return Sink
	 */
	public final Sink sink(final Sink sink) {
		return new Sink() {
			@Override
			public void write(Buffer source, long byteCount) throws IOException {
				checkOffsetAndCount(source.size, 0, byteCount);

				while (byteCount > 0L) {
					// Count how many bytes to write. This loop guarantees we
					// split on a segment boundary.
					long toWrite = 0L;
					for (Segment s = source.head; toWrite < TIMEOUT_WRITE_SIZE; s = s.next) {
						int segmentSize = s.limit - s.pos;
						toWrite += segmentSize;
						if (toWrite >= byteCount) {
							toWrite = byteCount;
							break;
						}
					}

					// Emit one write. Only this section is subject to the
					// timeout.
					boolean throwOnTimeout = false;
					enter();
					try {
						sink.write(source, toWrite);
						byteCount -= toWrite;
						throwOnTimeout = true;
					} catch (IOException e) {
						throw exit(e);
					} finally {
						exit(throwOnTimeout);
					}
				}
			}

			@Override
			public void flush() throws IOException {
				boolean throwOnTimeout = false;
				enter();
				try {
					sink.flush();
					throwOnTimeout = true;
				} catch (IOException e) {
					throw exit(e);
				} finally {
					exit(throwOnTimeout);
				}
			}

			@Override
			public void close() throws IOException {
				boolean throwOnTimeout = false;
				enter();
				try {
					sink.close();
					throwOnTimeout = true;
				} catch (IOException e) {
					throw exit(e);
				} finally {
					exit(throwOnTimeout);
				}
			}

			@Override
			public Timeout timeout() {
				return AsyncTimeout.this;
			}

			@Override
			public String toString() {
				return "AsyncTimeout.sink(" + sink + ")";
			}
		};
	}

	/**
	 * Returns a new source that delegates to {@code source}, using this to
	 * implement timeouts. This works best if {@link #timedOut} is overridden to
	 * interrupt {@code sink}'s current operation.
	 * @param source Source
	 * @return Source
	 */
	public final Source source(final Source source) {
		return new Source() {
			@Override
			public long read(Buffer sink, long byteCount) throws IOException {
				boolean throwOnTimeout = false;
				long start = System.nanoTime();
				enter();
				if(ILOG.isDebugEnabled()) {
					ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
							+ " - Method: enter()");
				}
				try {

					start = System.nanoTime();
					long result = source.read(sink, byteCount);
					if(ILOG.isDebugEnabled()) {
						ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
								+ " - Method: source.read(); result: " + result + "; byteCount: " + byteCount);
					}

					throwOnTimeout = true;
					return result;
				} catch (IOException e) {
					throw exit(e);
				} finally {

					start = System.nanoTime();
					exit(throwOnTimeout);
					if(ILOG.isDebugEnabled()) {
						ILOG.debug("Thread : " + Thread.currentThread().getName() + " - " + (System.nanoTime() - start)
								+ " - Method: exit()");
					}
				}
			}

			@Override
			public void close() throws IOException {
				boolean throwOnTimeout = false;
				try {
					source.close();
					throwOnTimeout = true;
				} catch (IOException e) {
					throw exit(e);
				} finally {
					exit(throwOnTimeout);
				}
			}

			@Override
			public Timeout timeout() {
				return AsyncTimeout.this;
			}

			@Override
			public String toString() {
				return "AsyncTimeout.source(" + source + ")";
			}
		};
	}

	/**
	 * Throws an IOException if {@code throwOnTimeout} is {@code true} and a
	 * timeout occurred. See {@link #newTimeoutException(IOException)}
	 * for the type of exception thrown.
	 */
	final void exit(boolean throwOnTimeout) throws IOException {
		boolean timedOut = exit();
		if (timedOut && throwOnTimeout)
		    // fix findbugs NP_NONNULL_PARAM_VIOLATION
			throw new InterruptedIOException("timeout");
	}

	/**
	 * Returns either {@code cause} or an IOException that's caused by
	 * {@code cause} if a timeout occurred. See
	 * {@link #newTimeoutException(IOException)} for the type of
	 * exception returned.
	 */
	final IOException exit(IOException cause) throws IOException {
		if (!exit())
			return cause;
		return newTimeoutException(cause);
	}

	/**
	 * Returns an {@link IOException} to represent a timeout. By default this
	 * method returns {@link InterruptedIOException}. If {@code cause}
	 * is non-null it is set as the cause of the returned exception.
	 * @param cause IOException
	 * @return IOException
	 */
	protected IOException newTimeoutException(IOException cause) {
		InterruptedIOException e = new InterruptedIOException("timeout");
		if (cause != null) {
			e.initCause(cause);
		}
		return e;
	}

	private static final class Watchdog extends Thread {
		private Locker locker;
		
		Watchdog(Locker locker) {
			super("Okio Watchdog : " + locker.toString());
			setDaemon(true);
			this.locker = locker;
		}

		public void run() {
			while (true) {
				try {
					AsyncTimeout timedOut;
					synchronized (locker) {
						timedOut = awaitTimeout(locker);

						// Didn't find a node to interrupt. Try again.
						if (timedOut == null)
							continue;

						// The queue is completely empty. Let this thread exit
						// and let another watchdog thread
						// get created on the next call to scheduleTimeout().
						if (timedOut == locker.head) {
							locker.head = null;
							return;
						}
					}

					// Close the timed out node.
					timedOut.timedOut();
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	/**
	 * Removes and returns the node at the head of the list, waiting for it to
	 * time out if necessary. This returns if there was no node at
	 * the head of the list when starting, and there continues to be no node
	 * after waiting {@code IDLE_TIMEOUT_NANOS}. It returns null if a new node
	 * was inserted while waiting. Otherwise this returns the node being waited
	 * on that has been removed.
	 */
	static AsyncTimeout awaitTimeout(Locker locker) throws InterruptedException {
	    // fix findbugs UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR
	    if (null == locker.head) {
            return null;
        }
	    
		// Get the next eligible node.
		AsyncTimeout node = locker.head.next;

		// The queue is empty. Wait until either something is enqueued or the
		// idle timeout elapses.
		if (node == null) {
			long startNanos = System.nanoTime();
			locker.wait(IDLE_TIMEOUT_MILLIS);
			return locker.head.next == null && (System.nanoTime() - startNanos) >= IDLE_TIMEOUT_NANOS ? locker.head // The
																										// idle
																										// timeout
																										// elapsed.
					: null; // The situation has changed.
		}

		long waitNanos = node.remainingNanos(System.nanoTime());

		// The head of the queue hasn't timed out yet. Await that.
		if (waitNanos > 0) {
			// Waiting is made complicated by the fact that we work in
			// nanoseconds,
			// but the API wants (millis, nanos) in two arguments.
			long waitMillis = waitNanos / 1000000L;
			waitNanos -= (waitMillis * 1000000L);
			locker.wait(waitMillis, (int) waitNanos);
			return null;
		}

		// The head of the queue has timed out. Remove it.
		locker.head.next = node.next;
		node.next = null;
		return node;
	}
}
