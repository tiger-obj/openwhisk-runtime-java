package ch.ethz.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;


public class MemAllocator {
    private static final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();

    private static String run(int size, int id) {
        //use strong reference to hold objects in arraylist
        String key = "tmp"+Thread.currentThread().getId();
        List<byte[]> junk = new ArrayList<>(10);
        cache.put(key,junk);
        junk.add(new byte[10*size]);
        String res = key + cache.get(key).hashCode();
        cache.remove(key);

        return res;
    }

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
    	ConcurrentHashMap<String, Object> cglobals = (ConcurrentHashMap<String, Object>) globals;
    	String hash = null;
        long time = System.currentTimeMillis();
        if (args.has("size")) {
            hash = run(args.getAsJsonPrimitive("size").getAsInt(),id);
        }
        JsonObject response = new JsonObject();
        response.addProperty("hash", hash != null ? hash : "Fail to get hash, check server log.");
        response.addProperty("time", System.currentTimeMillis() - time);
        return response;
    }
}


