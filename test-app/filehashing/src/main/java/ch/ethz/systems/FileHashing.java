package ch.ethz.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;
import com.google.gson.JsonObject;

public class FileHashing {
    private static final int size = 2*1024*1024;
    private static final String storage = "127.0.0.1:9000";

    private static MinioClientHttpDriver createDriver() {
        try {
            return new MinioClientHttpDriver("keykey","secretsecret",storage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO - is this Minioclient thread safe?
    private static String run(MinioClientHttpDriver driver, int seed, byte[] buffer) {
        try {
            InputStream stream = driver.getObject("files", String.format("file-%d.dat", seed));
            for (int bytesread = 0 , iter=0;
                 bytesread < size;
                 bytesread += stream.read(buffer, bytesread, size - bytesread));
            stream.close();
            return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(buffer));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static MinioClientHttpDriver getConn(ConcurrentHashMap<String, Object> cglobals) {
        MinioClientHttpDriver driver = null;
        String key = String.format("minio-%d",Thread.currentThread().getId());
        if (!cglobals.containsKey(key)) {
            driver = createDriver();
            cglobals.put(key, driver);
        } else {
            driver = (MinioClientHttpDriver) cglobals.get(key);
        }
        return driver;
    }


    private static byte[] getBuffer(ConcurrentHashMap<String, Object> cglobals) {
        byte[] buffer = null;
        String key = String.format("buffer-%d",Thread.currentThread().getId());
        if (!cglobals.containsKey(key)) {
            buffer = new byte[size];
            cglobals.put(key, buffer);
        } else {
            buffer = (byte[]) cglobals.get(key);
        }
        return buffer;
    }

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
        System.out.println("FHStart: "+System.nanoTime());
        ConcurrentHashMap<String, Object> cglobals = (ConcurrentHashMap<String, Object>) globals;
        String hash = null;
        long time = System.currentTimeMillis();
    
        if (args.has("seed")) {
            hash = run(getConn(cglobals), args.getAsJsonPrimitive("seed").getAsInt(), getBuffer(cglobals));
        }
    
        JsonObject response = new JsonObject();
        response.addProperty("hash", hash!=null?hash:"Fail to get hash, check server log.");
        response.addProperty("time", System.currentTimeMillis() - time);
        System.out.println("FHDone: "+System.nanoTime());
        return response;
    }
}