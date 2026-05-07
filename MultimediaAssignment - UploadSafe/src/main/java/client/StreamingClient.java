package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class StreamingClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 5000;

    private static final int TCP_STREAM_PORT = 6000;
    private static final int UDP_STREAM_PORT = 6001;
    private static final int RTP_STREAM_PORT = 6002;

    public static void main(String[] args) {
        System.out.println("Starting Streaming Client...");

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            System.out.println("Connected to server.");

            //Create input/output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            //Read welcome message from server
            String serverMessage = in.readLine();
            System.out.println("Message from server: " + serverMessage);

            //Perform speed test
            System.out.println("Measuring connection speed...");
            double speed = SpeedTester.measureDownloadSpeedMbps();
            System.out.println("Detected download speed: " + speed + " Mbps");

            //Select format
            System.out.println("Select format:");
            System.out.println("1. mp4");
            System.out.println("2. mkv");
            System.out.println("3. avi");

            int formatChoice = scanner.nextInt();
            String format;
            switch(formatChoice){
                case 1:
                    format = "mp4";
                    break;

                case 2:
                    format = "mkv";
                    break;

                case 3:
                    format = "avi";
                    break;

                default:
                    System.out.println("Invalid choice. Defaulting to mp4.");
                    format = "mp4";
            }

            //Send request to server
            out.println("FILTER_REQUEST");
            out.println(speed);
            out.println(format);

            //Receive filtered list
            int videoCount = Integer.parseInt(in.readLine());

            if(videoCount == 0){
                System.out.println("No videos available on the server.");
                return;
            }


            //Read and Display video list
            String[] videos = new String[videoCount];
            System.out.println("\nAvailable videos:");
            for(int i=0; i<videoCount; i++){
                videos[i] = in.readLine();
                System.out.println((i+1) + ". " + videos[i]);
            }

            //Ask user to select video file
            System.out.print("\nSelect a video by number: ");
            int choice = scanner.nextInt();

            if(choice >= 1 && choice <= videoCount){
                String selectedVideo = videos[choice-1];
                System.out.println("You selected: " + selectedVideo);

                System.out.println("\nSelect Transmission Protocol:");
                System.out.println("0. Auto");
                System.out.println("1. TCP");
                System.out.println("2. UDP");
                //System.out.println("3. RTP/UDP");

                int protocolChoice = scanner.nextInt();
                String protocol;

                switch(protocolChoice){
                    case 1:
                        protocol = "TCP";
                        break;
                    case 2:
                        protocol = "UDP";
                        break;
                   // case 3:
                     //   protocol = "RTP/UDP";
                    //    break;
                    case 0:
                    default:
                        protocol = getAutoProtocol(selectedVideo);
                        System.out.println("Auto-selected Protocol: " + protocol);
                        break;
                }

                out.println(selectedVideo);
                out.println(protocol);

                String serverResponse = in.readLine();

                if("START_STREAM".equals(serverResponse)){
                    String confirmedProtocol = in.readLine();
                    int streamPort = Integer.parseInt(in.readLine());

                    String clientCommand = buildStreamingClientCommand(confirmedProtocol, streamPort);

                    System.out.println("\nServer: stream is ready!");
                    System.out.println("Protocol: " + confirmedProtocol);
                    System.out.println("Port: " + streamPort);

                    System.out.println("\nStarting player with command:");
                    System.out.println(clientCommand);

                    //Execute FFplay
                    runFFplayCommand(clientCommand);

                } else {
                    System.out.println("Server could not start streaming.");
                }

            } else {
                out.println("INVALID_SELECTION");
                out.println("NONE");
                System.out.println("Invalid selection.");
            }

        } catch (IOException e){
            System.out.println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getAutoProtocol(String filename){
        if(filename.contains("240p") || filename.contains("360p") || filename.contains("480p")){
            return "UDP";
        } else {
            return "TCP";
        
        }
    }

    private static String buildStreamingClientCommand(String protocol, int port){
         switch (protocol) {
            case "TCP":
                return "ffplay tcp://127.0.0.1:" + port;

            case "UDP":
                return "ffplay -fflags nobuffer udp://127.0.0.1:" + port;

            case "RTP/UDP":
                return "ffplay rtp://127.0.0.1:" + port;

            default:
                return "Unsupported protocol: " + protocol;
        }
    }

    private static void runFFplayCommand(String command){
        try{
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.inheritIO(); //show ffplay output
            pb.start();
        } catch(IOException e) {
            System.out.println("Failed to start FFplay: " + e.getMessage());
        }
    }
    
}
