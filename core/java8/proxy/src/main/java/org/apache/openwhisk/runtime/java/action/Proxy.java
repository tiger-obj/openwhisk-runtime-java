/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openwhisk.runtime.java.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.lang.management.ManagementFactory;
import javassist.ClassPool;
import javassist.Loader;

public class Proxy {
    private HttpServer server;

    private ClassLoader loader = null;

    private SimpleTranslator translator = new SimpleTranslator();

    private Method main = null;

    private ConcurrentHashMap<String, Object> globals = new ConcurrentHashMap<>();

    public Proxy(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), -1);

        this.server.createContext("/init", new InitHandler());
        this.server.createContext("/run", new RunHandler());
        this.server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
    }

    private static Path saveBase64EncodedFile(InputStream encoded) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();

        InputStream decoded = decoder.wrap(encoded);

        File destinationFile = File.createTempFile("useraction", ".jar");
        destinationFile.deleteOnExit();
        Path destinationPath = destinationFile.toPath();

        Files.copy(decoded, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return destinationPath;
    }

    private void prepareMain(String entrypoint) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        final String[] splittedEntrypoint = entrypoint.split("#");
        final String entrypointClassName = splittedEntrypoint[0];
        final String entrypointMethodName = splittedEntrypoint.length > 1 ? splittedEntrypoint[1] : "main";

        Class<?> mainClass = loader.loadClass(entrypointClassName);

        main = mainClass.getMethod(entrypointMethodName, new Class[] { JsonObject.class, Map.class, int.class });
        main.setAccessible(true);
        int modifiers = main.getModifiers();
        if (main.getReturnType() != JsonObject.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new NoSuchMethodException("main");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void augmentEnv(Map<String, String> newEnv) {
        try {
            for (Class cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(System.getenv());
                    Map<String, String> map = (Map<String, String>) obj;
                    map.putAll(newEnv);
                }
            }
        } catch (Exception e) {}
    }

    private static void writeResponse(HttpExchange t, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void writeError(HttpExchange t, String errorMessage) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("error", errorMessage);
        writeResponse(t, 502, message.toString());
    }

    private static void writeLogMarkers() {
        // System\.out\.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        // System.err.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        System.out.flush();
        System.err.flush();
    }

    private class InitHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if (loader != null) {
                String errorMessage = "Cannot initialize the action more than once.";
                System.err.println(errorMessage);
                Proxy.writeError(t, errorMessage);
                return;
            }

            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonElement ie = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
                JsonObject inputObject = ie.getAsJsonObject();

                if (inputObject.has("value")) {
                    JsonObject message = inputObject.getAsJsonObject("value");
                    if (message.has("main") && message.has("code")) {
                        String mainClass = message.getAsJsonPrimitive("main").getAsString();
                        String base64Jar = message.getAsJsonPrimitive("code").getAsString();

                        // FIXME: this is obviously not very useful. The idea is that we
                        // will implement/use a streaming parser for the incoming JSON object so that we
                        // can stream the contents of the jar straight to a file.
                        InputStream jarIs = new ByteArrayInputStream(base64Jar.getBytes(StandardCharsets.UTF_8));

                        // Save the bytes to a file.
                        Path jarPath = saveBase64EncodedFile(jarIs);

                        // Start up the custom classloader.
                        ClassPool pool = ClassPool.getDefault();

                        // Add the application jar to the class pool
                        pool.appendClassPath(jarPath.toAbsolutePath().toString());
                        loader = new Loader(pool);

                        // Delegating classes to the default classloader.
                        ((Loader)loader).delegateLoadingOf("com.google.gson.");
                        ((Loader)loader).delegateLoadingOf("org.apache.openwhisk.runtime.java.action.");
                        ((Loader)loader).delegateLoadingOf("org.xmlpull.");
                        ((Loader)loader).delegateLoadingOf("okhttp3.");
                        //for logger impl not found due to apache httpclient
                        // ((Loader)loader).delegateLoadingOf("org.apache.http.");
                        ((Loader)loader).delegateLoadingOf("org.apache.http.");
                        //java 11 bug
                        ((Loader)loader).delegateLoadingOf("jdk.internal.reflect.");
                        // ((Loader)loader).delegateLoadingOf("javax.xml.bind");
                        
                        

                        // Add a translator to apply transformations to the loaded classes.
                        // TODO - there is a bug when loading minio!
                        ((Loader)loader).addTranslator(pool, translator);

                        // Find the main method and prepare it for activations.
                        prepareMain(mainClass);

                        Proxy.writeResponse(t, 200, "OK");
                        return;
                    }
                }

                Proxy.writeError(t, "Missing main/no code to execute.");
                return;
            } catch (Exception e) {
                e.printStackTrace(System.err);
                writeLogMarkers();
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
                return;
            }
        }
    }

    private class RunHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            // long initialMemory = printMemoryUsage("HeapBefore: ", 0);
        // System\.out\.println("HandleRequest: "+System.nanoTime());
            if (loader == null) {
                Proxy.writeError(t, "Cannot invoke an uninitialized action.");
                return;
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            SecurityManager sm = System.getSecurityManager();

            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonObject body = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))).getAsJsonObject();
                JsonObject inputObject = body.getAsJsonObject("value");
            // System\.out\.println("ParseRequest: "+System.nanoTime());
                HashMap<String, String> env = new HashMap<String, String>();
                Set<Map.Entry<String, JsonElement>> entrySet = body.entrySet();
                for(Map.Entry<String, JsonElement> entry : entrySet){
                    try {
                        if(!entry.getKey().equalsIgnoreCase("value"))
                            env.put(String.format("__OW_%s", entry.getKey().toUpperCase()), entry.getValue().getAsString());
                    } catch (Exception e) {}
                }

                // We always give a new classloader for a new invocation. It is used as an identifier.
                Thread.currentThread().setContextClassLoader(new Loader(loader, ClassPool.getDefault()));
                System.setSecurityManager(new WhiskSecurityManager());
            // System\.out\.println("ArgsReady: "+System.nanoTime());
                // Prepare environment.
                augmentEnv(env);
            // System\.out\.println("EnvReady: "+System.nanoTime());
            long EnvReady = System.nanoTime();
                // TODO - make this call lazy.
                translator.callStaticInitialisers(loader);
            // System\.out\.println("StaticInitialized: "+System.nanoTime());
            // System.err.println("StaticInitialized time: "+(System.nanoTime()-EnvReady)/1e6);
                globals.put("time", new Date().getTime());
            // System\.out\.println("LaunchJob: "+System.nanoTime());
                // User code starts running here.
                JsonObject output = (JsonObject) main.invoke(null, inputObject, globals, Thread.currentThread().getContextClassLoader().hashCode());
                // User code finished running here.
            // System\.out\.println("JobDone: "+System.nanoTime());
                if (output == null) {
                    throw new NullPointerException("The action returned null");
                }

                Proxy.writeResponse(t, 200, output.toString());
            // System\.out\.println("Response: "+System.nanoTime());
                // printMemoryUsage("HeapAfter: ", initialMemory);
                return;
            } catch (InvocationTargetException ite) {
                // These are exceptions from the action, wrapped in ite because
                // of reflection
                Throwable underlying = ite.getCause();
                underlying.printStackTrace(System.err);
                Proxy.writeError(t,
                        "An error has occured while invoking the action (see logs for details): " + underlying);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
            } finally {
                // System\.out\.println("AfterJob: "+System.nanoTime());
                writeLogMarkers();
                System.setSecurityManager(sm);
                Thread.currentThread().setContextClassLoader(cl);
            }
            
        }
    }
    private static long printMemoryUsage(String message, long initialMemory) {
//        TODO
        // System.gc();
        long currentMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        System.err.println(message + currentMemory / 1024 + " KByte" + (initialMemory == 0 ? "" : "  (difference: " + (currentMemory - initialMemory) / 1024 + " KByte)"));
        return currentMemory;
    }
    public static void main(String args[]) throws Exception {
        Proxy proxy = new Proxy(8080);
        proxy.start();
    }
}
