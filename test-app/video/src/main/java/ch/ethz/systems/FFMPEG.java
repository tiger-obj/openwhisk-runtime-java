package ch.ethz.systems;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
// import net.bramp.ffmpeg.FFmpeg;
// import net.bramp.ffmpeg.FFmpegExecutor;
// import net.bramp.ffmpeg.builder.FFmpegBuilder;

public class FFMPEG {
    private static final String storage = "http://172.18.0.2:9000";
	public static MinioClientHttpDriver driver = null;

	private static String readAllBytes(String filePath){
        String content = "";
        try{
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e) {}
        return content;
    }

	public static double current_utilization_runtime(){
        String meminfo = readAllBytes("/proc/meminfo").replace("\n", "");
        //total
        Pattern p = Pattern.compile("MemTotal: *(.*?) kB");
        Matcher m = p.matcher(meminfo);
        m.find();
        long memory_total = Long.parseLong(m.group(1));

        //free
        p = Pattern.compile("MemAvailable: *(.*?) kB");
        m = p.matcher(meminfo);
        m.find();
        long memory_free = Long.parseLong(m.group(1));
        return ((memory_total - memory_free)/1000.);
        //return (long)((memory_total)/1000.);
    }

	private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

		try (FileOutputStream outputStream = new FileOutputStream(file)) {

			int read;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
		}

	}

	public static void init_classifier() {
		try {
			// cls.load_model(ResourceUtils.getInputStream("tf_models/tensorflow_inception_graph.pb"));
			driver = new MinioClientHttpDriver(storage, "keykey", "secretsecret");
			InputStream is = driver.getObject("files","ffmpeg");
			copyInputStreamToFile(is,new File("ffmpeg"));
			Process process = Runtime.getRuntime().exec("chmod +x ./ffmpeg");
			process.waitFor();
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String sStackTrace = sw.toString(); // stack trace as a string
			throw new RuntimeException("1" + sStackTrace);
		}
	}

	private static void ffmpeg(String fileName) throws Exception{
		// FFmpeg ffmpeg = new FFmpeg("./ffmpeg");

		// FFmpegBuilder builder = new FFmpegBuilder()

        //     .setInput(fileName)     // Filename, or a FFmpegProbeResult
        //     .overrideOutputFiles(true) // Override the output if it exists

        //     .addOutput("out"+fileName)   // Filename for the destination
        //     .setFormat("mp4")        // Format is inferred from filename, or can be set
        //     // .setVideoResolution(640, 480) // at 640x480 resolution
        //     .setVideoResolution(64,48)
        //     .addExtraArgs("-t", "1")
        //     .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
        //     .done();

		// FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

		// // Run a one-pass encode
		// executor.createJob(builder).run();
        // long start = System.currentTimeMillis();
        Runtime rt = Runtime.getRuntime();
        // long getRuntime = System.currentTimeMillis();
        Process process = rt.exec("./ffmpeg -y -v error -i "+fileName+" -strict experimental -f mp4 -s 64x48 -t 1 out"+fileName);
        // System.out.println("Launch: "+(System.currentTimeMillis()-getRuntime)+"ms");
        process.waitFor();
        // System.out.println("Ffmpeg: "+(System.currentTimeMillis()-start)+"ms");
        //still need delayed tear down
        new File("out"+fileName).delete();
	}

	public static void transform(int id) {
		try {
            // long start = System.currentTimeMillis();
			InputStream is = driver.getObject("files","911511005.mp4");
			String fileName = Integer.toString(id) + ".mp4";
			copyInputStreamToFile(is, new File(fileName));
            // System.out.println("VideoDownload: "+(System.currentTimeMillis()-start)+"ms");
			ffmpeg(fileName);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String sStackTrace = sw.toString(); // stack trace as a string
			throw new RuntimeException("2" + sStackTrace);
		}

	}

	public static void main(String[] args) throws IOException {
//        HashMap<String, Object> m = new HashMap<String, Object>();
        main(new JsonObject(),null,0);
	}

	// public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
	// 	boolean slow_start = true;
	// 	double m0 = current_utilization_runtime();
	// 	synchronized (globals) {
	// 		if (!globals.containsKey("minio")) {
	// 			init_classifier();
	// 			globals.put("minio", minioClient);
	// 		} else {
	// 			minioClient = (MinioClient) globals.get("minio");
	// 			slow_start = false;
	// 		}
	// 	}
	// 	double m1 = current_utilization_runtime();

	// 	transform(id);

	// 	double m2 = current_utilization_runtime();
	// 	JsonObject response = new JsonObject();
	// 	long runtime_mem= Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	// 	response.addProperty("slow_start", slow_start ? 1 : 0);
	// 	response.addProperty("runtime_m", Double.toString(runtime_mem/1000000.));
	// 	response.addProperty("m0", Double.toString(m1));
	// 	response.addProperty("m1", Double.toString(m1));
	// 	response.addProperty("m2", Double.toString(m2));
	// 	response.addProperty("exists", new File("ffmpeg").exists());
	// 	return response;
	// }

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
		boolean slow_start = true;
        long start = System.currentTimeMillis();
		synchronized (globals) {
			if (!globals.containsKey("minio")) {
				init_classifier();
				globals.put("minio", driver);
			} else {
				driver = (MinioClientHttpDriver) globals.get("minio");
				slow_start = false;
			}
		}
        // System.out.println("Synchronization: "+(System.currentTimeMillis()-start)+"ms");

        id = (int) Thread.currentThread().getId();
		transform(id);
		JsonObject response = new JsonObject();
		response.addProperty("slow_start", slow_start ? 1 : 0);
        response.addProperty("response time", System.currentTimeMillis()-start);
		response.addProperty("exists", new File("ffmpeg").exists());
		return response;
    }	
}
