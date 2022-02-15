# Preparation
Please pre-install Java, Java 8 or above recommended. You can check your Java version with `java -version`.

## Install SDK
### Add dependencies to Maven project
```
<dependency>
  <groupId>com.chinanetcenter.wcs.sdk</groupId>
  <artifactId>wcs-java-sdk-v2</artifactId>
  <version>1.0.3</version>
</dependency>
```
### Import jars on Intellij IDEA

You may install as follows:

1. Download Java SDK package from github.
2. decompress the package.
3. Copy wcs-java-sdk-rest-v2-1.0.0.jar and all jar packages under `lib` to your project.
4. Select your project on Intellij IDEA: File->Project Structure->Modules->Dependencies->+->JARs or directories.
5. Select all the jars you copied on step 3, import them to External Libraries.

### Initialization
`WosClient` is a Java client, which is used to manage bucket and object.When using Java SDK, you should initialize and adjust default settings in `WosConfiguration` first.

**example**
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
```

### Create an instance of WosClient
```
WosClient wosClient = new WosClient(ak, sk, config, regionName);

//close wosClient
wosClient.close();
```

Note: `Endpoint` should be specified when creating an WosClient. You can have one or more WosClients and use them concurrently in your project.

## Configure WosClient

You can use `WosConfiguration` to configure `WosClient`, including timeout, maximum numbers of connection, etc. See details in the list below:

| Configuration | Description | method |
| -- | -- | -- |
|connectionTimeout | Set timeout for HTTP/HTTPS connetion (unit:ms), 60000 by default.	| WosConfiguration.setConnectionTimeout
|socketTimeout | Set timeout for Socket(unit:ms), 60000 by default.	| WosConfiguration.setSocketTimeout
|idleConnectionTime| 	An idle connection will be closed if it has been not working for a time more than idleConnectionTime (unit:ms). 30000 by default.	| WosConfiguration.setIdleConnectionTime
|maxIdleConnections	| Maximum number of idle connections, 1000 by default.	| WosConfiguration.setMaxIdleConnections
|maxConnections	| Maximum number of concurrencies for HTTP request, 1000 by default. | WosConfiguration.setMaxConnections
|maxErrorRetry	| Retries when request failed, 3 by default.| WosConfiguration.setMaxErrorRetry
|endPoint| 	Service address to access Object Storage, which may contain protocol, domain name and port. For example: "https://your-endpoint:443"	| WosConfiguration.setEndPoint
|readBufferSize	| Buffer size of Socket stream when reading (unit:byte), -1 by default, indicating no buffer.	| WosConfiguration.setReadBufferSize
|writeBufferSize	| Buffer size of Socket stream when writing(unit:byte), -1 by default, indicating no buffer.	| WosConfiguration.setWriteBufferSize
|pathStyle | Style of domain name as `bucketName.endpoint` is used when pathStyle is false, otherwise, 'endpoint/bucketName' style is used. |WosConfiguration.setPathStyle

# Quick Start

## 1. Upload an object

```
wosClient.putObject("bucketname", "objectname", new ByteArrayInputStream("Hello WOS".getBytes()));
```

## 2. Get an object
```
WosObject wosObject = wosClient.getObject("bucketname", "objectname");
InputStream content = wosObject.getObjectContent();
if (content != null)
{
    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
    while (true)
    {
        String line = reader.readLine();
        if (line == null)
            break;
        System.out.println("\n" + line);
    }
    reader.close();
}
```
Note：
- An instance of WosObject with contents and properties will be returned after calling `wosObject.getObject`
- You can get content of an object by calling `wosObject.getObjectContent`. Remember to close it when finished.

## 3. List objects in a bucket

```
ObjectListing objectListing = wosClient.listObjects("bucketname");
for(WosObject wosObject : objectListing.getObjects()){
    System.out.println(" - " + wosObject .getObjectKey() + "  " +  "(size = " + wosObject .getMetadata().getContentLength() + ")");
}
```
Note：
- An instance of ObjectListing is returned after calling `wosClient.listObjects`. You can get information of objects by calling `ObjetListing.getObjects`.
- 1000 objects will be listed by default.

## 4. Delete an object

```
wosClient.deleteObject("bucketname", "objectname");
```
## 5. Get URL with signature

```
TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
req.setBucketName(bucketName);
req.setObjectKey(objectKey);
TemporarySignatureResponse res = wosClient.createTemporarySignature(req);
System.out.println("Getting object using temporary signature url:");
System.out.println("\t" + res.getSignedUrl());
```

## 6. An example of WosClient

```
// You may keep only one global instance of WosClient.
// WosClient is thread-safe, which can be used concurrently.
WosClient wosClient = null; 
try
{
    String endPoint = "https://your-endpoint";
    String ak = "*** Provide your Access Key ***";
    String sk = "*** Provide your Secret Key ***";
    // Create an instance of WosClient
    wosClient = new WosClient(ak, sk, endPoint, regionName);
    // Put an object to the bucket
    HeaderResponse response = wosClient.putObject("bucketname", "objectname", new File("localfile"));  
    System.out.println(response);
}
catch (WosException e)
{
    System.out.println("HTTP Code: " + e.getResponseCode());
    System.out.println("Error Code:" + e.getErrorCode());
    System.out.println("Error Message: " + e.getErrorMessage());
    
    System.out.println("Request ID:" + e.getErrorRequestId());
    System.out.println("Host ID:" + e.getErrorHostId());
}finally{
    // Close the client. It's not required to close the client after calling if it's a global instance of WosClient.
    // WosClient is unavailable after calling WosClient.close()
    if(wosClient != null){
        try
        {
            // wosClient.close();
        }
        catch (IOException e)
        {
        }
    }
}
```

# Bucket Management
## 1. List buckets
You can list the buckets with `WosClient.listBuckets`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

// List buckets
List<WosBucket> buckets = wosClient.listBuckets();
for(WosBucket bucket : buckets){
    System.out.println("BucketName:" + bucket.getBucketName());
    System.out.println("CreationDate:" + bucket.getCreationDate());
    System.out.println("Endpoint:" + bucket.getEndpoint());
}
```
Note：
- The bucket list is ordered by bucket name dictionary.

## 2. Query existence of a bucket
You know whether a bucket exits by calling `WosClient.headBucket`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

boolean exists = wosClient.headBucket("bucketname");
```

# Upload

Object is the basic unit in Object Storage. There are some ways to put objects to Object Storage:

- Stream upload
- Object upload
- Multipart upload
- Form-based upload

Object within 5GB can be put to Object Storage by SDK.

For **stream upload** and **object upload**, the object to upload should not be over 5GB.

For large objects, please use **multipart upload**. And every part to upload should not be  over 5GB as well.

**Form-based upload** allows to upload by way of browser form.

SDK supports to upload objects of 0~5GB. For stream upload and object upload, content size should not be over 5GB; if you want to upload a large file, please use multipart upload. And form-based upload provides a way to upload objects based on browser forms.

Anonymous users can access the objects when read permission is set to  anonymous user’s reading. This way, the URL of an object will be like: https://bucket name.domain name/folder name/object name.

## 1. Stream upload
##java.io .InputStream## is used as the resource for stream upload. You can use 'WosClient.putObject' to perform stream upload.

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);


String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

String content = "Hello WOS";
wosClient.putObject("bucketname", "objectname", new ByteArrayInputStream(content.getBytes()));

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

InputStream inputStream = new URL("http://www.a.com").openStream();
wosClient.putObject("bucketname", "objectname", inputStream);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

FileInputStream fis = new FileInputStream(new File("localfile"));  // 待上传的本地文件路径，需要指定到具体的文件名
wosClient.putObject("bucketname", "objectname", fis);
```
Note:
- For large localfiles, you should always use object upload not stream upload.
- For files over 5GB, multipart upload is recommended.

## 2. File upload
File upload takes local files as resources.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

wosClient.putObject("bucketname", "objectname", new File("localfile")); // localfile为待上传的本地文件路径，需要指定到具体的文件名
```
Note:
-  Files to upload should not be larger than 5GB.

## 3. Create a folder
There is no 'folder' in Object Storage, what stored in bucket are objects actually. When you created a 'folder', you have created an object whose size is 0 and name ends by '/'. These 'folders' have no difference with objects, you can download and delete them as well. And they are showed as folders you see on Object Storage.

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

final String keySuffixWithSlash = "parent_directory/";
wosClient.putObject("bucketname", keySuffixWithSlash, new ByteArrayInputStream(new byte[0]));


wosClient.putObject("bucketname", keySuffixWithSlash + "objectname", new ByteArrayInputStream("Hello WOS".getBytes()));
```
Note:
- Just create the last level of multi-level folders, for example, to create an folder "src1/src2/src3/", no need to create "src1/", "src1/src2/".

## 4. Set properties of the object
You can set properties of an object before upload, including length, MD5 value, customized metadata, etc.

For details of properties, see in the following:

| Name | Description | Default value |
| -- | -- | -- |
|Object length </br>(Content-Length) | The length of the uploaded object will be truncated if it exceeds the length of the stream/file.	| Actual length of stream/file
|Object MIME type </br>(Content-Type) | The MIME type of the object defines the type of the object and the encoding of the web page, and determines in what form and encoding the browser will read the object.	| application/octet-stream
|Object MD5 value </br>(Content-MD5)| The MD5 value of the object data (encoded by Base64) is provided to the WOS server to verify the integrity of the data. | N/A
|Customized metadata	| The user describes the custom attributes of the objects uploaded to the space in order to customize the management of the objects. | N/A

## Set object length
You can set the object length with `ObjectMetadata.setContentLength`. 
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectMetadata metadata = new ObjectMetadata();
metadata.setContentLength(1024 * 1024L);// 1MB
wosClient.putObject("bucketname", "objectname", new File("localfile"), metadata);
```

## Set MIME type
You can set MIME type with `ObjectMetadata.setContentType`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectMetadata metadata = new ObjectMetadata();
metadata.setContentType("image/jpeg");
wosClient.putObject("bucketname", "objectname.jpg", new File("localimage.jpg"), metadata);
```
Note: If MIME type is not set, SDK will set MIMI type by the suffix of an object. For example, "application/xml" is set as MIME type of ".xml"; "text/html" is set as MIME type of ".html".

## Set MD5 value
You can set MD5 with `ObjectMetadata.setContentMd5`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectMetadata metadata = new ObjectMetadata();
metadata.setContentMd5("your md5 which should be encoded by base64");
wosClient.putObject("bucketname", "objectname", new File("localimage.jpg"), metadata);
```
Note:

- MD5 value must be Base64 encoded
- 'HTTP 400' is returned when MD5 does not match with MD5 computed by object content.
- MD5 is not verified when MD5 of the object is not set.
- You can directly compute the Content-MD5 header field through 'WosClient.base64Md5'.

## Set customized metadata
You can set customized metadata with `ObjectMetadata.addUserMetadata`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectMetadata metadata = new ObjectMetadata();
metadata.addUserMetadata("property1", "property-value1");
metadata.getMetadata().put("property2", "property-value2");
wosClient.putObject("bucketname", "objectname", new File("localfile"), metadata);
```
Note:
- In the code above, two customized metadata are set, they are “property1” and “property2”.
- An object can have multiple metadata,but total size of which cannot exceed 8KB.
- You can get customized metadata with 'WosClient.getObjectMetadata', see details in Get Metadata.
- Customized metadata will be download with the object when using 'WosClient.getObject'.

## 5. Multipart upload
For large files, you can use multipart upload to put them to Object Storage. It's recommended to use multipart upload when:
- Files to upload is larger than 100MB.
- Poor network.
- It's uncertain about size of file before upload.

**Strengths**
- Higher throughput—You can upload concurrently to improve the throughput.
- Quick recover from network abnormal.
- Pause and restart the upload at anytime—There is no expiration for an multipart upload task, so you have to cancel it initiatively if you want to stop it.
- You can start upload without knowing size of the object.

**How to multipart upload**
1. Initialization(WosClient.initiateMultipartUpload).
2. Upload the parts one by one or concurrently.(WosClient.uploadPart).
3. Merge the parts(WosClient.completeMultipartUpload) or cancel multipart upload(WosClient.abortMultipartUpload).

**Initialization**

You have to initiate a multipart upload request and make some configs with it first. An Upload ID is returned after that, it is used to identify this multipart upload task. Upload ID is also required for operations like canceling tasks, listing tasks, and listing parts, etc.

You can ininiate a multipart upload with `WosClient.initiateMultipartUpload`.

Parameters are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|bucketName | Bucket name.	| initiateMultipartUpload.setBucketName
|objectKey | The MIME type of the Set the object name to which the multipart upload task belongs.	| initiateMultipartUpload.setObjectKey
|expires| Set the expiration time of the final object generated by the multipart upload task, a positive integer. | initiateMultipartUpload.setExpires
|metadata	| Set object properties, support content-type, user-defined metadata. | initiateMultipartUpload.setMetadata

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest("bucketname", "objectname");
ObjectMetadata metadata = new ObjectMetadata();
metadata.addUserMetadata("property", "property-value");
metadata.setContentType("text/plain");
request.setMetadata(metadata);
InitiateMultipartUploadResult result = wosClient.initiateMultipartUpload(request);

String uploadId = result.getUploadId();
System.out.println("\t" + uploadId);
```
Note:
- Specify name and belonged bucket of the object with `InitiateMultipartUploadRequest`.
- You can set MIME type, customized metadata, etc with `InitiateMultipartUploadRequest`.
- Upload ID, which is returned by `InitiateMultipartUploadResult.getUploadId`, is the only global identification for a multipart upload task. And it is required in many APIs of multipart upload.

### Upload the parts
You can start the upload with an specified object name and Upload ID after initialization . For the "parts" to upload, each part has a **Part Number**(range 1~10000) to identify itself. A part number not only identify the part of an Upload ID, but also locate the part in object. If you upload a new part with a part number, then the old part of the number is overwritten.

Each part should be of 100KB~5GB except the last part of object, which is of 0~5GB.

You don't have to upload the parts in sequence of part number, you can even upload them in different processes or different servers. Ojbect Storage will make the parts into one complete object in order finally.

You can upload the parts with `WosClient.uploadPart`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

List<PartEtag> partEtags = new ArrayList<PartEtag>();

UploadPartRequest request = new UploadPartRequest("bucketname", "objectname");

request.setUploadId(uploadId);

request.setPartNumber(1);

request.setFile(new File("localfile"));

request.setPartSize(5 * 1024 * 1024L);
UploadPartResult result = wosClient.uploadPart(request);
partEtags.add(new PartEtag(result.getEtag(), result.getPartNumber()));

request = new UploadPartRequest("bucketname", "objectname");

request.setUploadId(uploadId);

request.setPartNumber(2);

request.setFile(new File("localfile"));

request.setOffset(5 * 1024 * 1024L);

request.setPartSize(5 * 1024 * 1024L);
result = wosClient.uploadPart(request);
partEtags.add(new PartEtag(result.getEtag(), result.getPartNumber()));
```
Note:
- Each part should exceed 100KB except the last one. But SDK will not verify the size of each part when uploading. The size is verified only when making the parts into one object.
- Object Storage will return the ETag(MD5) it gets to user.
- For safety, you can set `UploadPartRequest.setAttachMd5` at true and put it to `Content-MD5` header to compute MD5 of a part automaticlly(only useful when resource is localfile). Object Storage will compare MD5 of object with MD5 computed to ensure data integrity.
- You can also set MD5 of object with `UploadPartRequest.setContentMd5`. If so, `UploadPartRequest.setAttachMd5` will not work.
- Range of part numbers is 1~10000. If it exceeds this range, OS will return a 400 Bad Request error.

### Merge the parts

After all parts uploaded, you need to call `CompleteMultipartUploadRequest` to merge the parts. When performing this operation, you need to provide the part lists (including part numbers and ETag values). OS will verify the validity of each part, after all parts have been verified, OS will merge the parts into the final object.

Parameters are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|bucketName | Bucket name.	| initiateMultipartUpload.setBucketName
|objectKey | Set the object name to which the multipart upload task belongs.	| completeMultipartUpload.setObjectKey
|uploadId| Set the ID number of the multipart upload task. | completeMultipartUpload.setUploadId
|partEtag	| Set the list of segments to be merged. | completeMultipartUpload.setPartEtag

You can merge the parts with 'WosClient.completeMultipartUpload'.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

List<PartEtag> partEtags = new ArrayList<PartEtag>();

PartEtag part1 = new PartEtag();
part1.setPartNumber(1);
part1.seteTag("etag1");
partEtags.add(part1);


PartEtag part2 = new PartEtag();
part2.setPartNumber(2);
part2.setEtag("etag2");
partEtags.add(part2);

CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest("bucketname", "objectname", uploadId, partEtags);

wosClient.completeMultipartUpload(request);
```
Note:
- `partEtags` is a list of part numbers and Etags in code above.
- The parts can be discontinuous.

### Concurrently upload the parts

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
final String bucketName = "bucketname";
final String objectKey = "objectname";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);


ExecutorService executorService = Executors.newFixedThreadPool(20);
final File largeFile = new File("localfile");


InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
InitiateMultipartUploadResult result = wosClient.initiateMultipartUpload(request);

final String uploadId = result.getUploadId();
System.out.println("\t"+ uploadId + "\n");


long partSize = 100 * 1024 * 1024L;
long fileSize = largeFile.length();


long partCount = fileSize % partSize == 0 ? fileSize / partSize : fileSize / partSize + 1;

final List<PartEtag> partEtags = Collections.synchronizedList(new ArrayList<PartEtag>());

for (int i = 0; i < partCount; i++)
{

    final long offset = i * partSize;

    final long currPartSize = (i + 1 == partCount) ? fileSize - offset : partSize;

    final int partNumber = i + 1;
    executorService.execute(new Runnable()
    {
        @Override
        public void run()
        {
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setObjectKey(objectKey);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setFile(largeFile);
            uploadPartRequest.setPartSize(currPartSize);
            uploadPartRequest.setOffset(offset);
            uploadPartRequest.setPartNumber(partNumber);
            
            UploadPartResult uploadPartResult;
            try
            {
                uploadPartResult = wosClient.uploadPart(uploadPartRequest);
                System.out.println("Part#" + partNumber + " done\n");
                partEtags.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
            }
            catch (WosException e)
            {
                e.printStackTrace();
            }
        }
    });
}


executorService.shutdown();
while (!executorService.isTerminated())
{
    try
    {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException e)
    {
        e.printStackTrace();
    }
}

CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partEtags);
wosClient.completeMultipartUpload(completeMultipartUploadRequest);
```
Note:
- Use `UploadPartRequest.setOffset` and `UploadPartRequest.setPartSize` to locate the start and end of an part in object.

### Cancel multipart upload task
When you canceled an multipart upload task, then you can not use the Upload ID of this task to do anything. And the parts already uploade will be deleted as well.

Parts are remained in the bucket when uploading or even failed. These parts will take your storage space, you may clean these useless parts by cancelling the multipart upload task.

You can cancel multipart upload task with 'WosClient.abortMultipartUpload'.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

AbortMultipartUploadRequest request = new AbortMultipartUploadRequest("bucketname", "objectname", uploadId);

wosClient.abortMultipartUpload(request);
```

### List uploaded parts

You can use `WosClient.listParts` to list the parts which have been uploaded successfully.

Parameters are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|bucketName | Bucket name.	| initiateMultipartUpload.setBucketName
|key | The name of the object to which the multipart upload task belongs.	| ListPartsRequest.setKey
|uploadId| The globally unique identifier of the multipart upload task is obtained from the result returned by WosClient.initiateMultipartUpload. | ListPartsRequest.setUploadId
|maxParts	| Indicates the maximum number of segments returned by listing the uploaded segments, that is, the number of segments in each page when paging. | ListPartsRequest.setMaxParts
|partNumberMarker	| Indicates the starting position of the segment to be listed. Only segments with Part Number greater than this parameter will be listed. | ListPartsRequest.setPartNumberMarker

**Simple list**
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListPartsRequest request = new ListPartsRequest("bucketname", "objectname");
request.setUploadId(uploadId);
ListPartsResult result = wosClient.listParts(request);

for(Multipart part : result.getMultipartList()){

   System.out.println("\t"+part.getPartNumber());

   System.out.println("\t"+part.getSize());

   System.out.println("\t"+part.getEtag());

   System.out.println("\t"+part.getLastModified());
}
```
Note:
- 1000 part information will be listed at most. If the parts of an Upload ID are more than 1000, than `ListPartsResult.isTruncated` in returned result is true, indicating that not all parts are listed as a result. You can get the start position for next list with `ListPartsResult.getNextPartNumberMarker`.
- Use page list if you want to list all parts of an Upload ID.

**List all parts**
As `WosClient.listParts` can only list at most 1000 parts of an Upload ID, if you want to list over 1000 parts, please refer to the following example:
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

      
ListPartsRequest request = new ListPartsRequest("bucketname", "objectname");
request.setUploadId(uploadId);
ListPartsResult result;

do{
    result = wosClient.listParts(request);
    for(Multipart part : result.getMultipartList()){

       System.out.println("\t"+part.getPartNumber());

       System.out.println("\t"+part.getSize());

       System.out.println("\t"+part.getEtag());

       System.out.println("\t"+part.getLastModified());
    }
    request.setPartNumberMarker(Integer.parseInt(result.getNextPartNumberMarker()));
}while(result.isTruncated());
```

### List multipart upload tasks
You can list multipart upload tasks with `WosClient.listMultipartUploads`.

Parameters are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|bucketName | Bucket name.	| initiateMultipartUpload.setBucketName
|prefix | The object name in the returned multipart upload task must be prefixed with prefix. | ListMultipartUploadsRequest.setPrefix
|delimiter| Characters used to group object names in multipart upload tasks. For tasks that contain delimiter in the object name, the string from the first character to the first delimiter in the object name (if the prefix is ​​specified in the request, the prefix must be removed from the object name) will be treated as a grouping commonPrefix returns. | ListMultipartUploadsRequest.setDelimiter
|maxUploads	| List the maximum number of multipart upload tasks, the value range is 1~1000, when it exceeds the range, it will be processed according to the default 1000. | ListMultipartUploadsRequest.setMaxUploads
|keyMarker	| Represents the multipart upload task after returning to the specified keyMarker when enumerating. | ListMultipartUploadsRequest.setKeyMarker
|uploadIdMarker	| It is meaningful only when used together with the keyMarker parameter. It is used to specify the starting position of the returned result, that is, the multipart upload task after the uploadIdMarker of the specified keyMarker is returned when enumerating. | ListMultipartUploadsRequest.setUploadIdMarker

### Simply list multipart upload tasks
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListMultipartUploadsRequest request = new ListMultipartUploadsRequest("bucketname");

MultipartUploadListing result = wosClient.listMultipartUploads(request);
for(MultipartUpload upload : result.getMultipartTaskList()){
    System.out.println("\t" + upload.getUploadId());
    System.out.println("\t" + upload.getObjectKey());
    System.out.println("\t" + upload.getInitiatedDate());
}
```
Note:
- Information of at most 1000 tasks is returned when using `listMultipartUploads`. If the number of tasks are more than 1000, than `MultipartUploadListing.isTruncated` is true, indicating that not all tasks are listed as a result. You can get the start position for next list with `MultipartUploadListing.getNextKeyMarker` and `MultipartUploadListing.getNextUploadIdMarker`.
- Use page list if you want to list all multipart upload tasks of a specified bucket.

### List all multipart upload tasks by page
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
String uploadId = "upload id from initiateMultipartUpload";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListMultipartUploadsRequest request = new ListMultipartUploadsRequest("bucketname");
MultipartUploadListing result;

do{
    result = wosClient.listMultipartUploads(request);
    for(MultipartUpload upload : result.getMultipartTaskList()){
        System.out.println("\t" + upload.getUploadId());
        System.out.println("\t" + upload.getObjectKey());
        System.out.println("\t" + upload.getInitiatedDate());
    }
    request.setKeyMarker(result.getNextKeyMarker());
    request.setUploadIdMarker(result.getNextUploadIdMarker());
}while(result.isTruncated());
```

## Form-based upload

Form-based upload is to upload the object to specified bucket by HTML forms, and the biggest size of the object can not exceed 5GB.

You can create parameters for Form-based upload by `WosClient.createPostSignature`. For more details, please refer to `PostObjectSample`. 

You can also upload by form as following steps:
- Use `WosClient.createPostSignature` to generate request parameters for authentication.
- Prepare HTML forms.
- Fill in the forms with parameters generated by first step.
- Select localfiles to upload by form.

Note: Parameters generated by `WosClient.createPostSignature` are:
- policy, that is, 'policy' field in the form.
- signature, that is, 'signature' field in the form.

The following code shows how to generate parameters for form-based upload:

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "http://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

PostSignatureRequest request = new PostSignatureRequest();

Map<String, Object> formParams = new HashMap<String, Object>();

formParams.put("x-wos-acl", "public-read");

formParams.put("content-type", "text/plain");

request.setFormParams(formParams);

request.setExpires(3600);
PostSignatureResponse response = wosClient.createPostSignature(request);
formParams.put("key", objectKey);
formParams.put("policy", response.getPolicy());
formParams.put("accesskeyid", ak);
formParams.put("x-wos-credential", response.getCredential());
formParams.put("x-wos-algorithm", response.getAlgorithm());
formParams.put("bucket", bucketName);
formParams.put("x-wos-date", response.getDate());
formParams.put("x-wos-signature", response.getSignature());


System.out.println("\t" + response.getPolicy());
System.out.println("\t" + response.getSignature());
```

An example of HTML form:
```
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>

<form action="http://bucketname.your-endpoint/" method="post" enctype="multipart/form-data">
Object key

<input type="text" name="key" value="objectname" />
<p>
ACL

<input type="text" name="x-wos-acl" value="public-read" />
<p>
Content-Type

<input type="text" name="content-type" value="text/plain" />
<p>

<input type="hidden" name="policy" value="*** Provide your policy ***" />

<input type="hidden" name="AccessKeyId" value="*** Provide your access key ***"/>

<input type="hidden" name="signature" value="*** Provide your signature ***"/>

<input name="file" type="file" />
<input name="submit" value="Upload" type="submit" />
</form>
</body>
</html>
```
Note:
- `policy` and `signature` in HTML are got from `WosClient.createPostSignature`.
- You can directly download the form HTML sample PostDemo.

## Download Object

WOS Java SDK provides varies of interfaces to download the object, you can download objects with `WosClient.getObject`.

### 1. Stream download

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

WosObject wosObject = WosClient.getObject("bucketname", "objectname");

System.out.println("Object content:");
InputStream input = wosObject.getObjectContent();
byte[] b = new byte[1024];
ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
int len;
while ((len=input.read(b)) != -1){
       bos.write(b, 0, len);
}
 
System.out.println(new String(bos.toByteArray()));
bos.close();
input.close();
```
Note:
- The instance of WosObject returned by `WosClient.getObject` contains bucket name, object name, object properties and input stream.
- You can copy content of object to memory or local file with operations on input stream.
- Do remember to close the input stream explicitly to avoid a memory leak.

## 2. Range download

You can set the range for download to download just part of the data. For example you have set the range of download at 0~1000, then data of 0th ~ 1000th bytes will be returned, including the 1000th, that is, a total of 1001 bytes of data. If the range is invalid, then the complete object is returned. The following code shows how to perform a range download:
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

GetObjectRequest request = new GetObjectRequest("bucketname", "objectname");

request.setRangeStart(0l);
request.setRangeEnd(1000l);
WosObject wosObject = wosClient.getObject(request);

byte[] buf = new byte[1024];
InputStream in = wosObject.getObjectContent();
for (int n = 0; n != -1; ) {
    n = in.read(buf, 0, buf.length);
}

in.close();
```
Note:
- The complete object will be returned when the range is invalid, for example, start or end is negative or range size is bigger than file size.
- You can use range download to get a large object concurrently, please reference to `ConcurrentDownloadObjectSample`.

## 3. Set limits for download

You can set one or more limits to an download task. The download will go successfully under the limits, otherwise, it fails and throw exceptions.

The limits you can set are as folows:

| Parameter | Description | method |
| -- | -- | -- |
|If-Modified-Since| If the modification time of the object is later than the time specified by the parameter value, the object content is returned, otherwise an exception is thrown.	| GetObjectRequest.setIfModifiedSince
|If-Unmodified-Since| TIf the modification time of the object is earlier than the time specified by the parameter value, the object content is returned, otherwise an exception is thrown. | GetObjectRequest.setIfUnmodifiedSince
|If-Match| If the ETag value of the object is the same as the parameter value, the object content is returned, otherwise an exception is thrown. | GetObjectRequest.setIfMatchTag
|If-None-Match	| If the ETag value of the object is not the same as the parameter value, the object content is returned, otherwise an exception is thrown. | GetObjectRequest.setIfNoneMatchTag

Note:
- "ETag" means "MD5".
- HTTP status `412 precondition failed` is thrown when `If-Unmodified-Since` is included and not fit or `If-Match` is included and not fit.
- HTTP status `304 Not Modified` is thrown when `If-Unmodified-Since` is included and not fit or `If-Match` is included and not fit.

The following code shows how to perform a download with limits:
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

GetObjectRequest request = new GetObjectRequest("bucketname", "objectname");
request.setRangeStart(0l);
request.setRangeEnd(1000l);

request.setIfModifiedSince(new SimpleDateFormat("yyyy-MM-dd").parse("2016-01-01"));
WosObject wosObject = wosClient.getObject(request);

wosObject.getObjectContent().close();
```

## 4. Rewrite the response header
Part of the HTTP/HTTPS response header information can be rewritten when download. The rewritable response header information is shown in the following table:

| Parameter | Description | method |
| -- | -- | -- |
|contentType | Rewrite the Content-Type in the HTTP/HTTPS respons.	| ObjectRepleaceMetadata.setContentType
|contentLanguage | Rewrite the Content-Language in the HTTP/HTTPS response.	| ObjectRepleaceMetadata.setContentLanguage
|expires| Rewrite Expires in HTTP/HTTPS response. | ObjectRepleaceMetadata.setExpires
|cacheControl	| Rewrite Cache-Control in HTTP/HTTPS response. | ObjectRepleaceMetadata.setCacheControl
|contentDisposition	| Rewrite the Content-Disposition in the HTTP/HTTPS response. | ObjectRepleaceMetadata.setContentDisposition
|contentEncoding	| Rewrite the Content-Encoding in the HTTP/HTTPS response. | ObjectRepleaceMetadata.setContentEncoding


**example**
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

GetObjectRequest request = new GetObjectRequest("bucketname", "objectname");
ObjectRepleaceMetadata replaceMetadata = new ObjectRepleaceMetadata();
replaceMetadata.setContentType("image/jpeg");
request.setReplaceMetadata(replaceMetadata);

WosObject wosObject = wosClient.getObject(request);
System.out.println(wosObject.getMetadata().getContentType());

wosObject.getObjectContent().close();
```

## 5. Get customized metadata

After downloading the object successfully, the customized metadata of the object will be returned. 
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

PutObjectRequest request = new PutObjectRequest("bucketname", "objectname");
ObjectMetadata metadata = new ObjectMetadata();
metadata.addUserMetadata("property", "property-value");
request.setMetadata(metadata);
wosClient.putObject(request);

WosObject wosObject = wosClient.getObject("bucketname", "objectname");
System.out.println(wosObject.getMetadata().getUserMetadata("property"));

wosObject.getObjectContent().close();
```

# Object management
## 1. Set properties of the object
You can set properties of the object with `WosClient.setObjectMetadata`.(Customized metadata is involved)
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

SetObjectMetadataRequest request = new SetObjectMetadataRequest("bucketname", "objectname");
request.addUserMetadata("property1", "property-value1");

ObjectMetadata metadata = wosClient.setObjectMetadata(request);

System.out.println("\t" + metadata.getUserMetadata("property1"));
```

## 2. Get properties of the object

You can get the properties of the object with `WosClient.getObjectMetadata`, including MIME type, customized metada, etc.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectMetadata metadata = wosClient.getObjectMetadata("bucketname", "objectname");
System.out.println("\t" + metadata.getContentType());
System.out.println("\t" + metadata.getContentLength());
System.out.println("\t" + metadata.getUserMetadata("property"));
```

## 3. Get access permission 

You can get the access permission of an object with `WosClient.getObjectAcl`.

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

AccessControlList acl = wosClient.getObjectAcl("bucketname", "objectname");
System.out.println(acl);
```

## 4. List objects
You can list objects in a bucket with `WosClient.listObjects`.

Parameters are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|bucketName | Name of the bucket.	| ListObjectsRequest.setBucketName
|prefix | The object name returned by the limit must be prefixed with prefix.	| ListObjectsRequest.setPrefix
|marker| Enumerate the starting position of the object, and the returned object list will be all objects after the parameter after the object name is sorted in lexicographic order. | ObjectRepleaceMetadata.setExpires
|maxKeys	| Enumerate the maximum number of objects, the value range is 1~1000, when it exceeds the range, it will be processed according to the default 1000. | ListObjectsRequest.setMaxKeys
|delimiter	| The character used to group object names. For an object that contains a delimiter in the object name, the string from the first character to the first delimiter in the object name (if the prefix is specified in the request, the prefix must be removed from the object name) will be treated as a grouping commonPrefix returns. | ListObjectsRequest.setDelimiter

### Simple list

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ObjectListing result = wosClient.listObjects("bucketname");
for(WosObject wosObject : result.getObjects()){
    System.out.println("\t" + wosObject.getObjectKey());
    System.out.println("\t" + wosObject.getOwner());
}
```
Note:
- At most 1000 objects are listed in one request. If there are over 1000 objects in an bucket, then `ObjectListing.isTruncated` is true, indicating that not all objects in listed in result returned and you can call `ObjectListing.getNextMarker` to get the start location for next list.
- Use paged list if you want to list all the objects in a bucket.

### List specified number of objects

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");

request.setMaxKeys(100);
ObjectListing result = wosClient.listObjects(request);
for(WosObject wosObject : result.getObjects()){
    System.out.println("\t" + wosObject.getObjectKey());
    System.out.println("\t" + wosObject.getOwner());
}
```

### List objects with specified prefix

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");

request.setMaxKeys(100);
request.setPrefix("prefix");
ObjectListing result = wosClient.listObjects(request);
for(WosObject wosObject : result.getObjects()){
    System.out.println("\t" + wosObject.getObjectKey());
    System.out.println("\t" + wosObject.getOwner());
}
```

### Start the list from a specified position

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");

request.setMaxKeys(100);
request.setMarker("test");
ObjectListing result = wosClient.listObjects(request);
for(WosObject wosObject : result.getObjects()){
    System.out.println("\t" + wosObject.getObjectKey());
    System.out.println("\t" + wosObject.getOwner());
}
```

### List all objects by page

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");

request.setMaxKeys(100);

ObjectListing result;
do{
    result = wosClient.listObjects(request);
    for(WosObject wosObject : result.getObjects()){
        System.out.println("\t" + wosObject.getObjectKey());
        System.out.println("\t" + wosObject.getOwner());
    }
    
    request.setMarker(result.getNextMarker());
}while(result.isTruncated());
```

### List all objects in a folder
There is no 'folder' in Object Storage, what stored in bucket are objects actually. When you created a 'folder', you have created an object whose size is 0 and name ends by '/'. Using this folder object name as a prefix can simulate the function of list objects in the folder. The following code shows how to list objects in a folder:
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");
// Set prefix at "dir/"
request.setPrefix("dir/");
request.setMaxKeys(1000);

ObjectListing result;

do{
    result = wosClient.listObjects(request);
    for (WosObject wosObject : result.getObjects())
    {
        System.out.println("\t" + wosObject.getObjectKey());
        System.out.println("\t" + wosObject.getOwner());
    }
    request.setMarker(result.getNextMarker());
}while(result.isTruncated());
```

### List all objects grouped by folder

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ListObjectsRequest request = new ListObjectsRequest("bucketname");
request.setMaxKeys(1000);

request.setDelimiter("/");
ObjectListing result = wosClient.listObjects(request);
System.out.println("Objects in the root directory:");
for(WosObject wosObject : result.getObjects()){
    System.out.println("\t" + wosObject.getObjectKey());
    System.out.println("\t" + wosObject.getOwner());
}
listObjectsByPrefix(wosClient, request, result);
```

### The following code is a simple of recursively lists objects in subfolders by listObjectsByPrefix：
```
static void listObjectsByPrefix(WosClient wosClient, ListObjectsRequest request, ObjectListing result) throws WosException
{
      for(String prefix : result.getCommonPrefixes()){
          System.out.println("Objects in folder [" + prefix + "]:");
          request.setPrefix(prefix);
          result  = wosClient.listObjects(request);
          for(WosObject wosObject : result.getObjects()){
              System.out.println("\t" + wosObject.getObjectKey());
              System.out.println("\t" + wosObject.getOwner());
          }
          listObjectsByPrefix(wosClient, request, result);
      }
}
```
Note:
- The above code does not consider the case where the number of objects in the folder exceeds 1000.
- Since we need to list the objects as well as subfolders in the folder and the folder always end with "/", so the delimiter is always "/".
- In each recursive return result, ObjectListing.getObjects contains the objects in the folder; ObjectListing.getCommonPrefixes contains the subfolders of the folder.

## 5. Delete objects
### Delete an object
You can delete an object with `WosClient.deleteObject`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);
wosClient.deleteObject("bucketname", "objectname");
```

### Delete objects in batch

You can delete objects(up to 1000 objects) in batch with `WosClient.deleteObjects`.

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";
WosClient wosClient = new WosClient(ak, sk, config, regionName);

DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest("bucketname");
wosClient.deleteObjects(deleteRequest);
```

## 6. Copy an object

With `WosClient.copyObject`, you can copy an object. You can reset the properties and access permission of the copy when call the interface. And conditional copying is supported.

Restrictions

- You must have the read permission for the object to be copied.
- Cross region replication is not supported with the interface.
- The object to be copied should not exceed 5GB. When the size is under 1GB, use simple copy please; otherwise, use multipart copy.

### Simple copy

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

try{
    CopyObjectResult result = wosClient.copyObject("sourcebucketname", "sourceobjectname", "destbucketname", "destobjectname");

    System.out.println("\t" + result.getStatusCode());
    System.out.println("\t" + result.getEtag());
}
catch (WosException e)
{
    System.out.println("HTTP Code: " + e.getResponseCode());
    System.out.println("Error Code:" + e.getErrorCode());
    System.out.println("Error Message: " + e.getErrorMessage());

    System.out.println("Request ID:" + e.getErrorRequestId());
    System.out.println("Host ID:" + e.getErrorHostId());
}
```

### Set new metadata for the object

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

CopyObjectRequest request = new CopyObjectRequest("sourcebucketname", "sourceobjectname", "destbucketname", "destobjectname");

request.setReplaceMetadata(true);
ObjectMetadata newObjectMetadata = new ObjectMetadata();
newObjectMetadata.setContentType("image/jpeg");
newObjectMetadata.addUserMetadata("property", "property-value");
newObjectMetadata.setObjectStorageClass(StorageClassEnum.WARM);
request.setNewObjectMetadata(newObjectMetadata);
CopyObjectResult result = wosClient.copyObject(request);
System.out.println("\t" + result.getEtag());
```
Note:
- To make the set, you have to use `CopyObjectRequest.setReplaceMetadata` as well as `CopyObjectRequest.setNewObjectMetadata`.

### Copy with restrictions
You can place one or more restrictions on the copy work. The copy works only when restrictions are followed, otherwise, an exception is thrown and the copy fails.

The restrictions that you can place are as follows:

| Parameter | Description | method |
| -- | -- | -- |
|Copy-Source-If-Modified-Since | If the modification time of the source object is later than the time specified by the parameter value, copy is performed, otherwise an exception is thrown.| CopyObjectRequest.setIfModifiedSince
|Copy-Source-If-Unmodified-Since | If the modification time of the source object is earlier than the time specified by the parameter value, copy is performed, otherwise an exception is thrown.| CopyObjectRequest.setIfUnmodifiedSince
|Copy-Source-If-Match| If the ETag value of the source object is the same as the value of this parameter, copy is performed, otherwise an exception is thrown. | CopyObjectRequest.setIfMatchTag
|Copy-Source-If-None-Match| If the ETag value of the source object is not the same as the parameter value, copy is performed, otherwise an exception is thrown. | CopyObjectRequest.setIfNoneMatchTag

Note:
- Etag value is MD5 of source object.
- HTTP status `412 precondition failed` is thrown when one of `Copy-Source-If-Unmodified-Since`, `Copy-Source-If-Match`, `Copy-Source-If-Modified-Since` and `Copy-Source-If-None-Match` is included and not fit.
- `Copy-Source-If-Modified-Since` and `Copy-Source-If-None-Match` can be used together; `Copy-Source-If-Unmodified-Since` and `Copy-Source-If-Match` can be used together.

```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

CopyObjectRequest request = new CopyObjectRequest("sourcebucketname", "sourceobjectname", "destbucketname", "destobjectname");

request.setIfModifiedSince(new SimpleDateFormat("yyyy-MM-dd").parse("2016-01-01"));
request.setIfNoneMatchTag("none-match-etag");

CopyObjectResult result = wosClient.copyObject(request);
System.out.println("\t" + result.getEtag());
```

### multipart copy

Multipart copy is a special case of multipart upload, that is, the part for multipart upload is got by copying existing object (or part of object) in a specified bucket. You can copy the parts with `WosClient.copyPart`.


```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

final String destBucketName = "destbucketname";
final String destObjectKey = "destobjectname";
final String sourceBucketName = "sourcebucketname";
final String sourceObjectKey = "sourceobjectname";

final WosClient wosClient = new WosClient(ak, sk, config, regionName);

ExecutorService executorService = Executors.newFixedThreadPool(20);

InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(destBucketName, destObjectKey);
InitiateMultipartUploadResult result = wosClient.initiateMultipartUpload(request);

final String uploadId = result.getUploadId();
System.out.println("\t"+ uploadId + "\n");

ObjectMetadata metadata = wosClient.getObjectMetadata(sourceBucketName, sourceObjectKey);

long partSize = 100 * 1024 * 1024L;
long objectSize = metadata.getContentLength();

long partCount = objectSize % partSize == 0 ? objectSize / partSize : objectSize / partSize + 1;

final List<PartEtag> partEtags = Collections.synchronizedList(new ArrayList<PartEtag>());

for (int i = 0; i < partCount; i++)
{
    final long rangeStart = i * partSize;

    final long rangeEnd = (i + 1 == partCount) ? objectSize - 1 : rangeStart + partSize - 1;

    final int partNumber = i + 1;
    executorService.execute(new Runnable()
    {
        
        @Override
        public void run()
        {
            CopyPartRequest request = new CopyPartRequest();
            request.setUploadId(uploadId);
            request.setSourceBucketName(sourceBucketName);
            request.setSourceObjectKey(sourceObjectKey);
            request.setDestinationBucketName(destBucketName);
            request.setDestinationObjectKey(destObjectKey);
            request.setByteRangeStart(rangeStart);
            request.setByteRangeEnd(rangeEnd);
            request.setPartNumber(partNumber);
            CopyPartResult result;
            try
            {
                result = wosClient.copyPart(request);
                System.out.println("Part#" + partNumber + " done\n");
                partEtags.add(new PartEtag(result.getEtag(), result.getPartNumber()));
            }
            catch (WosException e)
            {
                e.printStackTrace();
            }
        }
    });
}

executorService.shutdown();
while (!executorService.isTerminated())
{
    try
    {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException e)
    {
        e.printStackTrace();
    }
}

CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(destBucketName, destObjectKey, uploadId, partEtags);
wosClient.completeMultipartUpload(completeMultipartUploadRequest);
```

# Life cycle management

You can set life cycle rules for the bucket to make automatic type conversions or automatic elimination of expired objects. You can set multi-rules for the object with varies of prefixes.

A rule may have:

- Rule ID, which is used to identify a rule and cannot be repeated.
- Affected object prefix. This rule only applies to objects that match the prefix.
- The conversion strategy for the latest version of the object, specified as:
- Specifies that objects that meet the prefix are converted to the specified storage type on the first few days after they are created.
- Directly specify the date when objects satisfying the prefix are converted to the specified storage type.
- The expiration time of the latest version of the object, specified as:
- Specifies that objects that meet the prefix expire in the first few days after they are created.
- Directly specify the expiration date of the object that meets the prefix.
- Whether to take effect or not.

## 1.Set life cycle rules
You can set life cycle rules of an bucket with `WosClient.setBucketLifecycleConfiguration`.
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

final String ruleId = "delete obsoleted files";
final String matchPrefix = "obsoleted/";

StringBuffer sb = new StringBuffer();

LifecycleConfiguration lifecycleConfig = new LifecycleConfiguration();
LifecycleConfiguration.Rule rule = lifecycleConfig.new Rule();
rule.setEnabled(true);
rule.setId(ruleId);
rule.setPrefix(matchPrefix);
LifecycleConfiguration.Expiration expiration = lifecycleConfig.new Expiration();
expiration.setDays(10);

rule.setExpiration(expiration);
lifecycleConfig.addRule(rule);

sb.append("Setting bucket lifecycle\n\n");
HeaderResponse headerResponse = wosClient.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
System.out.println(headerResponse);
```

## 2.View life cycle rules
You can view life cycle rules of the bucket with `WosClient.getBucketLifecycle`. 
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

LifecycleConfiguration config = wosClient.getBucketLifecycle("bucketname");

for (Rule rule : config.getRules())
{
    System.out.println(rule.getId());
    System.out.println(rule.getPrefix());
    for(Transition transition : rule.getTransitions()){
        System.out.println(transition.getDays());
        System.out.println(transition.getStorageClass());
    }
    System.out.println(rule.getExpiration() != null ? rule.getExpiration().getDays() : "");
}
```

## 3.Delete life cycle rules
You can delete life cycle rules of the bucket with `WosClient.deleteBucketLifecycle`. 
```
WosConfiguration config = new WosConfiguration();
config.setSocketTimeout(30000);
config.setConnectionTimeout(10000);
config.setEndPoint(endPoint);

String endPoint = "https://your-endpoint";
String ak = "*** Provide your Access Key ***";
String sk = "*** Provide your Secret Key ***";

WosClient wosClient = new WosClient(ak, sk, config, regionName);

wosClient.deleteBucketLifecycle("bucketname");
```
