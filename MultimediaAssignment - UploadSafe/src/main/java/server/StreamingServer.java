package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StreamingServer {
    private static final int PORT = 5000;
    private static final String VIDEO_FOLDER = "videos";

    private static final Logger logger = ServerLogger.getLogger();

    private static final String[] SUPPORTED_FORMATS = {"mp4", "mkv", "avi"};
    private static final String[] SUPPORTED_RESOLUTIONS = {"240p", "360p", "480p", "720p", "1080p"};

    //Streaming endpoints for testing command generation
    private static final int TCP_STREAM_PORT = 6000;
    private static final int UDP_STREAM_PORT = 6001;
    private static final int RTP_STREAM_PORT = 6002;


    public static void main(String[] args) {
        logger.info("Starting Streaming Server...");

        List<VideoFile> parsedVideos = getParsedVideoList();

        logger.info("\nParsed Video FIles: ");
        for(VideoFile video : parsedVideos){
            logger.info(String.valueOf(video));
        }

        Map<String, List<VideoFile>> groupedVideos = groupVideosByMovie(parsedVideos);

        logger.info("\nGrouped Videos by Movie: ");
        for(String movieName : groupedVideos.keySet()){
            logger.info(movieName + " -> " + groupedVideos.get(movieName).size() + " file(s)");

        }

        List<MissingVideoVersion> missingVersions = findMissingVersions(groupedVideos);
        logger.info("\nMissing Versions that can be generated: ");
        if(missingVersions.isEmpty()){
            logger.info("No missing versions found!");
        }else{

            for(MissingVideoVersion missing : missingVersions){
                logger.info(String.valueOf(missing));
            }
        }

        List<ConversionTask> conversionTasks = buildConversionTasks(groupedVideos, missingVersions);

        logger.info("\nPlanned conversion tasks:");
        if(conversionTasks.isEmpty()){
            logger.info("No conversion tasks available.");
        }else{
            for(ConversionTask task : conversionTasks){
                logger.info(String.valueOf(task));
            }
        }

        

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server is listening on port " + PORT);

            while(true){
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, parsedVideos);
                handler.start();
            }
        }catch(IOException e){
               logger.info("Server Error: " + e.getMessage());
                e.printStackTrace();
        }
    }

    private static String buildStreamingServerCommand(String selectedVideo, String protocol){
        String inputPath = VIDEO_FOLDER + File.separator + selectedVideo;

         switch (protocol) {
            case "TCP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c copy -f mpegts tcp://127.0.0.1:" + TCP_STREAM_PORT + "?listen=1";

            case "UDP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c:v libx264 -c:a aac -f mpegts udp://127.0.0.1:" + UDP_STREAM_PORT;

            case "RTP/UDP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c copy -f rtp rtp://127.0.0.1:" + RTP_STREAM_PORT;

            default:
                return "Unsupported protocol: " + protocol;
        }
    }

    private static void runFFmpegCommand(String command){
        try{
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.inheritIO(); //show ffmpeh output in terminal
            pb.start();
        } catch(IOException e){
            System.out.println("Failed to start FFMPEG: " + e.getMessage());
        }
    }

    private static List<VideoFile> getParsedVideoList(){
        List<VideoFile> videoFiles = new ArrayList<>();
        File folder = new File(VIDEO_FOLDER);

        if(!folder.exists()){
            System.out.println("Video folder does not exist: " + VIDEO_FOLDER);
            return videoFiles;
        }

        if(!folder.isDirectory()){
            System.out.println(VIDEO_FOLDER + " is not a folder.");
            return videoFiles;
        }

        File[] files = folder.listFiles();

        if(files == null){
            System.out.println("Could not read files from folder.");
            return videoFiles;
        }

        for(File file : files){
            if(file.isFile() && isSupportedVideoFile(file.getName())){
                VideoFile parsedVideo = parseVideoFileName(file.getName());

                if(parsedVideo != null){
                    videoFiles.add(parsedVideo);
                }else{
                    System.out.println("Skipping invalid filename format: " + file.getName());
                }
            }
        }

        return videoFiles;
    }

    private static Map<String, List<VideoFile>> groupVideosByMovie(List<VideoFile> videos){
        Map<String, List<VideoFile>> grouped = new LinkedHashMap<>();

        for(VideoFile video : videos){
            String movieName = video.getMovieName();
            if(!grouped.containsKey(movieName)){
                grouped.put(movieName, new ArrayList<>());
            }
            grouped.get(movieName).add(video);
        }
        return grouped;
    }

    private static List<MissingVideoVersion> findMissingVersions(Map<String, List<VideoFile>> groupedVideos) {
        List<MissingVideoVersion> missingVersions = new ArrayList<>();

        for (String movieName : groupedVideos.keySet()) {
            List<VideoFile> movieFiles = groupedVideos.get(movieName);

            int maxResolutionIndex = getHighestResolutionIndex(movieFiles);

            for (int i = 0; i <= maxResolutionIndex; i++) {
                String resolution = SUPPORTED_RESOLUTIONS[i];

                for (String format : SUPPORTED_FORMATS) {
                    if (!versionExists(movieFiles, resolution, format)) {
                        missingVersions.add(new MissingVideoVersion(movieName, resolution, format));
                    }
                }
            }
        }

        return missingVersions;
    }

    private static List<ConversionTask> buildConversionTasks(Map<String, List<VideoFile>> groupedVideos, List<MissingVideoVersion> missingVersions){
        List<ConversionTask> tasks = new ArrayList<>();
        for(MissingVideoVersion missing : missingVersions){
            List<VideoFile> movieFiles = groupedVideos.get(missing.getMovieName());

            if(movieFiles == null || movieFiles.isEmpty()){
                continue;
            }

            VideoFile bestSource = findBestSourceForTarget(movieFiles, missing.getResolution());

            if(bestSource != null){
                tasks.add(new ConversionTask(bestSource, missing));
            }else{
                System.out.println("No valid source found for: " + missing.getExpectedFileName());
            }
        }

        return tasks;
    }

    private static VideoFile findBestSourceForTarget(List<VideoFile> movieFiles, String targetResolution){
        int targetIndex = getResolutionIndex(targetResolution);
        VideoFile bestSource = null;
        int bestSourceIndex = Integer.MAX_VALUE;

        for(VideoFile video : movieFiles){
            int sourceIndex = getResolutionIndex(video.getResolution());

            if(sourceIndex >= targetIndex && sourceIndex < bestSourceIndex){
                bestSource = video;
                bestSourceIndex = sourceIndex;
            }
        }

        return bestSource;
    }

    private static void executeConversionTasks(List<ConversionTask> tasks){
        for(ConversionTask task : tasks){
            boolean success = generateVideoVersion(task);

            if(success){
                System.out.println("Successfully generated: " + task.getTargetVersion().getExpectedFileName());
            }else{
                System.out.println("Failed to generate: " + task.getTargetVersion().getExpectedFileName());
            }
        }
    }

    private static boolean generateVideoVersion(ConversionTask task){
        String inputPath = VIDEO_FOLDER + File.separator + task.getSourceVideo().getFullFileName();
        String outputPath = VIDEO_FOLDER + File.separator + task.getTargetVersion().getExpectedFileName();
        String targetResolution = task.getTargetVersion().getResolution();
        String scaleValue = getFFmpegScale(targetResolution);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(inputPath);
        command.add("-vf");
        command.add("scale=" + scaleValue);
        command.add(outputPath);

        System.out.println("\nRunning Command:");
        System.out.println(String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try{
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while((line = reader.readLine()) != null){
                System.out.println("[FFMPEG] " + line);
            }

            int exitCode = process.waitFor();
            return exitCode == 0;
        }catch(IOException e){
            System.out.println("FFMPEG execution error: " + e.getMessage());
            return false;
        }catch (InterruptedException e){
            System.out.println("FFMPEG process interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

  private static String getFFmpegScale(String resolution){
    switch(resolution){
        case "240p":
            return "426:240";
        case "360p":
            return "640:360";
        case "480p":
            return "854:480";
        case "720p":
            return "1280:720";
        case "1080p":
            return "1920:1080";
        default:
            throw new IllegalArgumentException("Unsupported Resolution: " + resolution);

    }
  }  




private static int getHighestResolutionIndex(List<VideoFile> movieFiles){
    int maxIndex = -1;

    for(VideoFile video : movieFiles){
        int resolutionIndex = getResolutionIndex(video.getResolution());
        if(resolutionIndex > maxIndex){
            maxIndex = resolutionIndex;
        }
    }

    return maxIndex;
}

private static int getResolutionIndex(String resolution){
    for(int i=0; i<SUPPORTED_RESOLUTIONS.length; i++){
        if(SUPPORTED_RESOLUTIONS[i].equals(resolution)){
            return i;
        }
    }

    return -1;
}

private static boolean versionExists(List<VideoFile> movieFiles, String resolution, String format) {
        for (VideoFile video : movieFiles) {
            if (video.getResolution().equals(resolution) && video.getFormat().equals(format)) {
                return true;
            }
        }
        return false;
}

private static boolean isSupportedVideoFile(String filename){
    String lowerName = filename.toLowerCase();
    return lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".avi");
 
}

private static VideoFile parseVideoFileName(String filename){
    int dotIndex = filename.lastIndexOf('.');
    int dashIndex = filename.lastIndexOf('-');

    if(dotIndex == -1 || dashIndex == -1 || dashIndex > dotIndex){
        return null;
    }

    String movieName = filename.substring(0, dashIndex);
    String resolution = filename.substring(dashIndex + 1, dotIndex);
    String format = filename.substring(dotIndex + 1);

    if(!isValidResolution(resolution)){
        return null;
    }

    return new VideoFile(movieName, resolution, format, filename);

}

private static boolean isValidResolution(String resolution) {
    return resolution.equals("240p") ||
            resolution.equals("360p") ||
            resolution.equals("480p") ||
            resolution.equals("720p") ||
            resolution.equals("1080p");
}

public static List<VideoFile> filterVideos(List<VideoFile> videos, double speed, String format){
    List<VideoFile> result = new ArrayList<>();
    String maxResolution = getMaxResolutionForSpeed(speed);

    for(VideoFile video : videos){
        if(video.getFormat().equals(format) && isResolutionAllowed(video.getResolution(), maxResolution)){
            result.add(video);
        }
    }

    return result;
}

private static String getMaxResolutionForSpeed(double speed){
    if(speed < 1.5) return "240p";
    else if(speed < 3) return "360p";
    else if(speed < 5) return "480p";
    else if(speed < 8) return "720p";
    else return "1080p";
}

private static boolean isResolutionAllowed(String videoRes, String maxRes){
    return getResolutionIndex(videoRes) <= getResolutionIndex(maxRes);
}

}



               

            

           