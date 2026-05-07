package client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class StreamingClientGUI extends JFrame {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 5000;

    private Process currentPlayerProcess;

    private JLabel speedLabel;
    private JComboBox<String> formatBox;
    private DefaultListModel<String> videoListModel;
    private JList<String> videoList;
    private JComboBox<String> protocolBox;
    private JTextArea logArea;

    private double measuredSpeed = 5.0;

    public StreamingClientGUI() {
        setTitle("Streaming Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton speedButton = new JButton("Measure Speed");
        speedLabel = new JLabel("Detected speed: not measured");

        speedButton.addActionListener(e -> measureSpeed());

        speedPanel.add(speedButton);
        speedPanel.add(speedLabel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatBox = new JComboBox<>(new String[]{"mp4", "mkv", "avi"});
        JButton fetchButton = new JButton("Fetch Videos");

        fetchButton.addActionListener(e -> fetchVideos());

        filterPanel.add(new JLabel("Format:"));
        filterPanel.add(formatBox);
        filterPanel.add(fetchButton);

        topPanel.add(speedPanel);
        topPanel.add(filterPanel);

        videoListModel = new DefaultListModel<>();
        videoList = new JList<>(videoListModel);
        JScrollPane videoScrollPane = new JScrollPane(videoList);
        videoScrollPane.setPreferredSize(new Dimension(700, 180));

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        protocolBox = new JComboBox<>(new String[]{"Auto", "TCP", "UDP"});
        JButton playButton = new JButton("Start Stream");

        playButton.addActionListener(e -> startStream());

        JButton stopButton = new JButton("Stop Stream");
        stopButton.addActionListener(e -> stopCurrentStream());

        controlPanel.add(new JLabel("Protocol:"));
        controlPanel.add(protocolBox);
        controlPanel.add(playButton);
        controlPanel.add(stopButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(700, 180));

        bottomPanel.add(controlPanel, BorderLayout.NORTH);
        bottomPanel.add(logScrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);

    
        add(videoScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void measureSpeed() {
        appendLog("Measuring speed...");
        new Thread(() -> {
            measuredSpeed = SpeedTester.measureDownloadSpeedMbps();
            SwingUtilities.invokeLater(() -> {
                speedLabel.setText(String.format("Detected speed: %.2f Mbps", measuredSpeed));
                appendLog("Speed measured: " + measuredSpeed + " Mbps");
            });
        }).start();
    }

    private void fetchVideos() {
        String format = (String) formatBox.getSelectedItem();
        videoListModel.clear();

        appendLog("Connecting to server...");
        appendLog("Requesting filtered videos for format: " + format);

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String welcome = in.readLine();
                appendLog("Server: " + welcome);

                out.println("FILTER_REQUEST");
                out.println(measuredSpeed);
                out.println(format);

                int videoCount = Integer.parseInt(in.readLine());

                if (videoCount == 0) {
                    appendLog("No videos available for current speed/format.");
                    return;
                }

                for (int i = 0; i < videoCount; i++) {
                    String video = in.readLine();
                    videoListModel.addElement(video);
                }

                appendLog("Received " + videoCount + " video(s).");

            } catch (Exception e) {
                appendLog("Fetch error: " + e.getMessage());
            }
        }).start();
    }

    private void startStream() {
        String selectedVideo = videoList.getSelectedValue();

        if (selectedVideo == null) {
            appendLog("Please select a video first.");
            return;
        }

        String protocolChoice = (String) protocolBox.getSelectedItem();
        String protocol;

        if ("Auto".equals(protocolChoice)) {
            protocol = getAutoProtocol(selectedVideo);
        } else {
            protocol = protocolChoice;
        }

        appendLog("Selected video: " + selectedVideo);
        appendLog("Selected protocol: " + protocol);

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String welcome = in.readLine();
                appendLog("Server: " + welcome);

                out.println("FILTER_REQUEST");
                out.println(measuredSpeed);
                out.println(formatBox.getSelectedItem());

                int videoCount = Integer.parseInt(in.readLine());
                for (int i = 0; i < videoCount; i++) {
                    in.readLine();
                }

                out.println(selectedVideo);
                out.println(protocol);

                String serverResponse = in.readLine();

                if ("START_STREAM".equals(serverResponse)) {
                    String confirmedProtocol = in.readLine();
                    int streamPort = Integer.parseInt(in.readLine());

                    String clientCommand = buildStreamingClientCommand(confirmedProtocol, streamPort);

                    appendLog("Stream ready.");
                    appendLog("Protocol: " + confirmedProtocol);
                    appendLog("Port: " + streamPort);
                    appendLog("Launching player: " + clientCommand);

                    runFFplayCommand(clientCommand);
                } else {
                    appendLog("Server could not start streaming.");
                }

            } catch (Exception e) {
                appendLog("Streaming error: " + e.getMessage());
            }
        }).start();
    }

    private String getAutoProtocol(String filename) {
        if (filename.contains("240p") || filename.contains("360p") || filename.contains("480p")) {
            return "UDP";
        } else if (filename.contains("720p") || filename.contains("1080p")) {
            return "TCP";
        } else {
            return "TCP";
        }
    }

    private String buildStreamingClientCommand(String protocol, int port) {
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

    private void runFFplayCommand(String command) {
        try {
            if(currentPlayerProcess != null && currentPlayerProcess.isAlive()){
                appendLog("Stopping previous player...");
                currentPlayerProcess.destroyForcibly();
                currentPlayerProcess.waitFor();
            }

            String[] parts = command.split(" ");
            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.inheritIO();

            currentPlayerProcess = pb.start();
            appendLog("Player Started.");

        } catch (Exception e) {
            appendLog("Failed to start FFplay: " + e.getMessage());
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StreamingClientGUI gui = new StreamingClientGUI();
            gui.setVisible(true);
        });
    }

    private void stopCurrentStream(){
        try{
            if(currentPlayerProcess != null && currentPlayerProcess.isAlive()){
                currentPlayerProcess.destroyForcibly();
                appendLog("Stopped current stream!");

            }else{
                appendLog("No active streams detected.");
            }
        } catch (Exception e){
            appendLog("Failed to stop stream: " + e.getMessage());
        }
    }

    @Override
    public void dispose(){
        stopCurrentStream();
        super.dispose();
    }
    
}