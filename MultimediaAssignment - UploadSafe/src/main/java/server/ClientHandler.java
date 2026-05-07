package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;



public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final List <VideoFile> parsedVideos;
    private final int tcpStreamPort;
    private final int udpStreamPort;
    private final int rtpStreamPort;

    private static final Logger logger = ServerLogger.getLogger();

    public  ClientHandler(Socket clientSocket, List<VideoFile> parsedVideos){
        this.clientSocket = clientSocket;
        this.parsedVideos = parsedVideos;
        
        int basePort = PortManager.allocateBasePort();
        this.tcpStreamPort = basePort;
        this.udpStreamPort = basePort + 1;
        this.rtpStreamPort = basePort + 2;

    }

    @Override
    public void run(){
        ServerStats.clientConnected();
        System.out.println("Handling Client: " + clientSocket.getInetAddress());
        
        try(PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))){
        out.println("Welcome to the Streaming Server!");

        String request = in.readLine();
        logger.info("Client request: " + request);


        if("FILTER_REQUEST".equals(request)){
            double speed = Double.parseDouble(in.readLine());
            String format = in.readLine();

            logger.info("Client Speed: " + speed + " Mbps");
            logger.info("Client Format: " + format);

            List<VideoFile> filtered = StreamingServer.filterVideos(parsedVideos, speed, format);

            out.println(filtered.size());
            for(VideoFile video : filtered){
                out.println(video.getFullFileName());
            }

            String selectedVideo = in.readLine();
            String protocol = in.readLine();

            logger.info("Client selected: " + selectedVideo);
            logger.info("Selected protocol: " + protocol);

            if (!"INVALID_SELECTION".equals(selectedVideo) && !"NONE".equals(protocol)) {

                ServerStats.protocolUsed(protocol);

                int selectedPort = getPortForProtocol(protocol);

                String serverCommand = buildStreamingServerCommand(selectedVideo, protocol);

                logger.info("Assigned port: " + selectedPort);
                logger.info("Starting stream with command: " + serverCommand);

                out.println("START_STREAM");
                out.println(protocol);
                out.println(selectedPort);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                runFFmpegCommand(serverCommand);

            } else {
                out.println("INVALID_STREAM_REQUEST");
                logger.warning("Invalid stream request from client.");
            }
        }
    }catch(IOException e){
        logger.severe("Client handler error: " + e.getMessage());

    }finally{
        try{
            clientSocket.close();

        }catch(IOException e){
           logger.warning("Error closing client socket: " + e.getMessage());

        }

        ServerStats.clientDisconnected();
        logger.info("Client disconnected: " + clientSocket.getInetAddress() +
                " | Active clients: " + ServerStats.getActiveClients() +
                " | Total served: " + ServerStats.getTotalClientsServed() +
                " | TCP: " + ServerStats.getTcpStreams() +
                " | UDP: " + ServerStats.getUdpStreams() +
                " | RTP: " + ServerStats.getRtpStreams());

    }
      
    }

    private String buildStreamingServerCommand(String selectedVideo, String protocol){
        String inputPath = "videos" + java.io.File.separator + selectedVideo;
        switch (protocol) {
            case "TCP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c copy -f mpegts tcp://127.0.0.1:" + tcpStreamPort + "?listen=1";

            case "UDP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c:v libx264 -c:a aac -f mpegts udp://127.0.0.1:" + udpStreamPort;

            case "RTP/UDP":
                return "ffmpeg -re -i \"" + inputPath + "\" -c copy -f rtp rtp://127.0.0.1:" + rtpStreamPort;

            default:
                return "Unsupported protocol: " + protocol;
        }
    }
    
    private void runFFmpegCommand(String command){
        try{
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.inheritIO();
            pb.start();
        }catch (IOException e){
            System.out.println("Failed to start FFMPEG: " + e.getMessage());
        }
    }

    private int getPortForProtocol(String protocol){
        switch(protocol){
            case "TCP":
                return tcpStreamPort;
            case "UDP":
                return udpStreamPort;
            case "RTP/UDP":
                return rtpStreamPort;
            default:
                return tcpStreamPort;
        }
    }
}
