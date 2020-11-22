package ch.ethz.systems;

import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.lang.System;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MinioClientHttpDriver {
    private String accessKey;
    private String secretKey;
    private String storage;
    private CloseableHttpClient client = HttpClients.createDefault();
    MinioClientHttpDriver(String accessKey, String secretKey, String endpoint){
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.storage = endpoint;
    }
//
//    public static void main(){
//        try {
//            HttpUriRequest request = AWS4Signature.getUrlAuthInQuery("127.0.0.1:9000","/filehashing/file-1.dat","minioadmin","minioadmin");
//            System.out.println(request);
//            Header[] headerFields = request.getAllHeaders();
//            for(int e = 0; e<headerFields.length; e++){
//                System.out.println(headerFields[e].getName() + ": " + headerFields[e].getValue());
//            }
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//    }
    public InputStream getObject(String bucketName, String objectName) throws IOException {
        String canonicalUri = "/"+bucketName + "/"+objectName;
        try{
            AWS4Signature signaturer = new AWS4Signature();
            HttpUriRequest request = signaturer.getUrlAuthInQuery(storage,canonicalUri,accessKey,secretKey);
//            System.out.println(request);
//            Header[] headerFields = request.getAllHeaders();
//            for(int e = 0; e<headerFields.length; e++){
//                System.out.println(headerFields[e].getName() + ": " + headerFields[e].getValue());
//            }
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
}


//AWS-HMAC-SHA256
class AWS4Signature{
    private static final String HMAC_SHA256 = "HmacSHA256";
    private Mac sha256Hmac  = null;
    private static final String service = "s3";
    private static final String method = "GET";
    private static final String region = "";
    private MessageDigest digest = null;
    private byte[] sign(byte[] key, byte[] msg) {
        String result;
        try {
            if(sha256Hmac==null)
                sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_SHA256);
            sha256Hmac.init(keySpec);
            byte[] macData = sha256Hmac.doFinal(msg);
            // Can either base64 encode or put it right into hex
//            result = Base64.getEncoder().encodeToString(macData);
            StringBuilder sb = new StringBuilder();
//            for (byte b : macData) {
//                sb.append(String.format("%02X ", b));
//            }
//            result = bytesToHex(macData);
            return macData;
        } catch (InvalidKeyException|NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {}
        return new byte[0];
    }
    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName){
        byte[] kDate = sign(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = sign(kDate, regionName.getBytes(StandardCharsets.UTF_8));
        byte[] kService = sign(kRegion, serviceName.getBytes(StandardCharsets.UTF_8));
        byte[] kSigning = sign(kService, "aws4_request".getBytes(StandardCharsets.UTF_8));
        return kSigning;
    }
//    payload_hash = hashlib.sha256(('').encode('utf-8')).hexdigest()
    private String byteArrToHex(byte[] input){
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    private String hashHex(String msg){
        if(digest==null){
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        byte[] encodedHash = digest.digest(msg.getBytes(StandardCharsets.UTF_8));
        return byteArrToHex(encodedHash);
    }
    HttpUriRequest getUrlAuthInQuery(String host,String canonicalUri,String accessKey, String secretKey) throws URISyntaxException {
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
        String canonicalRequest = method + '\n' + canonicalUri + '\n' + canonicalQuerystring + '\n' + canonicalHeaders + '\n' + signedHeaders + '\n' + payloadHash;
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
}


