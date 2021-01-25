package ch.ethz.systems;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class MinioClientHttpDriver {
    private String accessKey;
    private String secretKey;
    private String storage;
    
    private CloseableHttpClient client;
    MinioClientHttpDriver(String endpoint,String accessKey, String secretKey){
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.storage = endpoint.split("//")[1];
        // System.out.println("stringFields:"+System.nanoTime());
        this.client = HttpClients.createDefault();
        // System.out.println("DefaultClient:"+System.nanoTime());
    }

    public InputStream getObject(String bucketName, String objectName) throws IOException {
        String canonicalUri = "/"+bucketName + "/"+objectName;
        try{
//            AWS4Signature signaturer = new AWS4Signature();
            HttpUriRequest request = AWS4Signature.getObjectHttpRequest(storage,canonicalUri,accessKey,secretKey);

            HttpResponse response = client.execute(request);
            if(response.getStatusLine().getStatusCode()!=200)
                throw new HttpException("Failure by querying Minio "+response.toString());
            HttpEntity entity = response.getEntity();
            return entity.getContent();
        }catch (URISyntaxException | HttpException e){
            e.printStackTrace(System.err);
        }finally {
        }
        return null;
    }
    //only for our use case, which is a small object of 3006 Byte
    public void putObject(String bucketName, String objectName, InputStream stream, String contentType,int size) throws IOException {
        if(size>5000){
            throw new IOException("not supported by current version driver");
        }
        String canonicalUri = "/"+bucketName + "/"+objectName;
        try{
//            AWS4Signature signaturer = new AWS4Signature();
            HttpUriRequest request = AWS4Signature.putObjectHttpRequest(storage,canonicalUri,accessKey,secretKey,size,contentType,stream);

            HttpResponse response = client.execute(request);
            if(response.getStatusLine().getStatusCode()!=200)
                throw new HttpException("Failure by querying Minio "+response.toString());
            HttpEntity entity = response.getEntity();
            return;
//            return entity.getContent();
        }catch (URISyntaxException | HttpException e){
            e.printStackTrace(System.err);
        }finally {
        }

    }
}


//AWS-HMAC-SHA256
class AWS4Signature{
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static Mac sha256Hmac  = null;
    private static final String service = "s3";
    private static final String region = "us-east-1";
    private static MessageDigest digest = null;
    private static final int CHUNK_SIGNATURE_METADATA_LEN = 85;
    // As final additional chunk must be like
    // 0;chunk-signature=b6c6ea8a5354eaf15b3cb7646744f4275b71ea724fed81ceb9323e279d449df9\r\n\r\n
    // the length is 86
    private static final int FINAL_ADDITIONAL_CHUNK_LEN = 1 + CHUNK_SIGNATURE_METADATA_LEN;
    static{
        try {
            sha256Hmac = Mac.getInstance(HMAC_SHA256);
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    private static byte[] sign(byte[] key, byte[] msg) {
        try {
//            if(sha256Hmac==null)
//                sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_SHA256);
            sha256Hmac.init(keySpec);
            byte[] macData = sha256Hmac.doFinal(msg);
            return macData;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } finally {}
        return new byte[0];
    }
    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName){
        byte[] kDate = sign(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = sign(kDate, regionName.getBytes(StandardCharsets.UTF_8));
        byte[] kService = sign(kRegion, serviceName.getBytes(StandardCharsets.UTF_8));
        byte[] kSigning = sign(kService, "aws4_request".getBytes(StandardCharsets.UTF_8));
        return kSigning;
    }
//    payload_hash = hashlib.sha256(('').encode('utf-8')).hexdigest()
    private static String byteArrToHex(byte[] input){
        return Hex.encodeHexString(input);
    }
    private static String hashHex(String msg){
//
//        if(digest==null){
//            try {
//                digest = MessageDigest.getInstance("SHA-256");
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            }
//        }
        byte[] encodedHash = digest.digest(msg.getBytes(StandardCharsets.UTF_8));
        return byteArrToHex(encodedHash);
    }
    static HttpUriRequest getObjectHttpRequest(String host,String canonicalUri,String accessKey, String secretKey) throws URISyntaxException {
        //get time in different format
        String amzdate;
        String dateStamp;
        SimpleDateFormat timeStampFormatter =  new SimpleDateFormat("yyyyMMddHHmmss");
        timeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        String timeStamp=timeStampFormatter.format(date);
        dateStamp = timeStamp.substring(0,8);

        amzdate = dateStamp + "T" + timeStamp.substring(8) + "Z";
        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);
        //payload hash
        String payload = "";
        String payloadHash = hashHex(payload);
        //signature
        String algo = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + "aws4_request";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonicalHeaders = "host:" + host + '\n'+"x-amz-content-sha256:" + payloadHash + "\n" + "x-amz-date:" + amzdate + "\n";
        String canonicalQuerystring = "";
        String canonicalRequest = "GET" + '\n' + canonicalUri + '\n' + canonicalQuerystring + '\n' + canonicalHeaders + '\n' + signedHeaders + '\n' + payloadHash;
        String stringToSign = algo + "\n" +  amzdate + "\n" +  credentialScope + "\n" +hashHex(canonicalRequest);
        String signature = byteArrToHex(sign(signingKey,stringToSign.getBytes(StandardCharsets.UTF_8)));
        String authorizationHeader = algo + ' ' + "Credential=" + accessKey + "/" + credentialScope + ", " +  "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(host)
                .setPath(canonicalUri)
                .build();
        HttpUriRequest request = RequestBuilder.get()
                .setUri(uri)
                .addHeader("Authorization", authorizationHeader)
                .addHeader("X-Amz-Content-Sha256", payloadHash)
                .addHeader("X-Amz-Date", amzdate)
                .build();
        return request;
    }
    static HttpUriRequest putObjectHttpRequest(String host,String canonicalUri,String accessKey, String secretKey, int size,String contentType,InputStream stream) throws URISyntaxException, IOException {
        //get time in different format
        String amzdate;
        String dateStamp;
        SimpleDateFormat timeStampFormatter =  new SimpleDateFormat("yyyyMMddHHmmss");
        timeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        String timeStamp=timeStampFormatter.format(date);
        dateStamp = timeStamp.substring(0,8);
        amzdate = dateStamp + "T" + timeStamp.substring(8) + "Z";
//Debug
//        amzdate = "20210113T000244Z";
        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);

        //payload hash
        String payload = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
//        String payloadHash = hashHex(payload);
        String payloadHash = payload;
        //signature
        String algo = "AWS4-HMAC-SHA256";
        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + "aws4_request";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date;x-amz-decoded-content-length";
        String decodedContentLength =String.valueOf(size);
        String canonicalHeaders = "host:" + host + '\n'+"x-amz-content-sha256:" + payloadHash + "\n" + "x-amz-date:" + amzdate + "\n"+ "x-amz-decoded-content-length:" + decodedContentLength + "\n";
        String canonicalQuerystring = "";
        String canonicalRequest = "PUT" + '\n' + canonicalUri + '\n' + canonicalQuerystring + '\n' + canonicalHeaders + '\n' + signedHeaders + '\n' + payloadHash;
        String stringToSign = algo + "\n" +  amzdate + "\n" +  credentialScope + "\n" +hashHex(canonicalRequest);
        String signature = byteArrToHex(sign(signingKey,stringToSign.getBytes(StandardCharsets.UTF_8)));
        String authorizationHeader = algo + ' ' + "Credential=" + accessKey + "/" + credentialScope + ", " +  "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost(host)
                .setPath(canonicalUri)
                .build();

//        CHUNK_SIZE_IN_HEX_STRING + ";chunk-signature=" + SIGNATURE + "\r\n" + CHUNK_DATA + "\r\n"
        int contentLength = Integer.toHexString(size).getBytes(StandardCharsets.UTF_8).length+CHUNK_SIGNATURE_METADATA_LEN+size+FINAL_ADDITIONAL_CHUNK_LEN;

        byte[] data = IOUtils.toByteArray(stream);
        digest.update((byte[]) data, 0, size);

        String chunkSha256 = byteArrToHex(digest.digest()).toLowerCase(Locale.US);
        String chunkStringToSign = "AWS4-HMAC-SHA256-PAYLOAD" + "\n"
                + amzdate + "\n"
                + credentialScope + "\n"
                + signature + "\n"
                + hashHex("") + "\n"
                + chunkSha256;
        String chunkSignature = byteArrToHex(sign(signingKey,chunkStringToSign.getBytes(StandardCharsets.UTF_8)));
        String finalToSign = "AWS4-HMAC-SHA256-PAYLOAD" + "\n"
                + amzdate + "\n"
                + credentialScope + "\n"
                + chunkSignature + "\n"
                + hashHex("") + "\n"
                + hashHex(""); //empty for final chunk
        String finalSignature = byteArrToHex(sign(signingKey,finalToSign.getBytes(StandardCharsets.UTF_8)));
//
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // Add metadata.
        os.write(Integer.toHexString(size).getBytes(StandardCharsets.UTF_8));
        os.write(";chunk-signature=".getBytes(StandardCharsets.UTF_8));
        os.write(chunkSignature.getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
        // Add chunk data.
        os.write(data, 0, data.length);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
        os.write(Integer.toHexString(0).getBytes(StandardCharsets.UTF_8));
        os.write(";chunk-signature=".getBytes(StandardCharsets.UTF_8));
        os.write(finalSignature.getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));

        byte[] chunkBody = os.toByteArray();
        ByteArrayEntity body = new ByteArrayEntity(chunkBody);
        body.setChunked(false);
        HttpUriRequest request = RequestBuilder.put()
                .setUri(uri)
                .addHeader("Authorization", authorizationHeader)
                .addHeader("X-Amz-Content-Sha256", payload)
                .addHeader("X-Amz-Date", amzdate)
                .addHeader("X-Amz-Decoded-Content-Length", decodedContentLength)
                .addHeader("Content-Type", contentType)
                .setEntity(body)
                .build();
        return request;
    }
}


