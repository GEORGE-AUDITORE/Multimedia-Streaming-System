# Multimedia Streaming System

A Java-based multimedia streaming platform built with a Client-Server architecture, supporting adaptive video streaming, multiple communication protocols, and a graphical user interface.

## Overview

This project was developed as part of a Multimedia and Multimedia Communications university assignment.

(For legal reasons, no files are currently included inside the videos folder. Feel free to add your own for testing.)

The system allows clients to:

- connect to a streaming server,
- measure their network speed,
- receive filtered video lists based on connection quality,
- choose streaming protocols,
- and watch videos through FFmpeg/FFplay streaming.

The application supports both:

- Terminal-based Client
- GUI-based Client (Java Swing)

---

# Features

## Server Features

- Multi-threaded client handling
- Dynamic port allocation
- Video parsing and filtering
- Adaptive streaming logic
- FFmpeg integration
- Logging and statistics
- Multiple streaming protocols

## Client Features

- Speed test integration
- Adaptive video selection
- TCP / UDP / RTP support
- GUI application with Swing
- Video playback through FFplay
- Stream start / stop support

---

# Technologies Used

| Technology | Purpose |
|------------|---------|
| Java | Core application |
| Java Sockets | Client-Server communication |
| Java Swing | GUI |
| Maven | Dependency management |
| FFmpeg | Video streaming |
| FFplay | Video playback |
| Multithreading | Multiple clients |
| Logger API | Logging system |

---

# Project Structure

```text
src/
├── main/
│   └── java/
│       ├── client/
│       │   ├── StreamingClient.java
│       │   ├── StreamingClientGUI.java
│       │   └── SpeedTester.java
│       │
│       ├── server/
│       │   ├── StreamingServer.java
│       │   ├── ClientHandler.java
│       │   ├── PortManager.java
│       │   ├── ServerLogger.java
│       │   └── ServerStats.java
│       │
│       └── model/
│           ├── VideoFile.java
│           ├── ConversionTask.java
│           └── MissingVideoVersion.java
│
├── videos/
└── pom.xml
```

---

# Requirements

Before running the project, install:

- Java 17+
- Maven
- FFmpeg
- FFplay

---

# FFmpeg Setup

Download FFmpeg:

https://ffmpeg.org/download.html

Add FFmpeg to your Windows PATH.

Verify installation:

```bash
ffmpeg -version
ffplay -version
```

---

# Build Instructions

## Compile Project

```bash
mvn compile
```

---

# Running the Server

```bash
mvn exec:java "-Dexec.mainClass=server.StreamingServer"
```

---

# Running the GUI Client

```bash
mvn exec:java "-Dexec.mainClass=client.StreamingClientGUI"
```

---

# Running the Terminal Client

```bash
mvn exec:java "-Dexec.mainClass=client.StreamingClient"
```

---

# Supported Protocols

| Protocol | Usage |
|----------|------|
| TCP | Stable high-resolution streaming |
| UDP | Fast low-latency streaming |
| RTP/UDP | Experimental RTP support |

---

# Adaptive Streaming Logic

The system dynamically filters videos based on measured client speed.

| Speed | Max Resolution |
|------|----------------|
| <1.5 Mbps | 240p |
| <3 Mbps | 360p |
| <5 Mbps | 480p |
| <8 Mbps | 720p |
| >=8 Mbps | 1080p |

---

# GUI Features

- Measure Connection Speed
- Fetch Available Videos
- Select Video Format
- Choose Streaming Protocol
- Start Stream
- Stop Stream
- Live Logging Panel

---

# Multi-Client Support

The server uses multithreading to support multiple simultaneous clients.

Each client connection is handled independently using:

```java
new ClientHandler(...).start();
```

---

# Logging & Statistics

The server tracks:

- Active clients
- Total clients served
- Protocol usage
- Connection events
- Streaming events

Logs are stored in:

```text
server.log
```

---

# Known Limitations

The following features were not fully implemented:

- SSL/TLS encryption
- Load balancing
- Stream recording on client side
- Real-time adaptive bitrate switching

---

# Challenges Faced

Some important development challenges included:

- FFmpeg PATH configuration
- Managing hanging FFplay processes
- UDP instability on 720p streams
- Swing GUI layout issues
- Dynamic port conflicts

These issues were resolved through process management improvements, layout fixes, and protocol optimization.

---

# Screenshots

## GUI Main Window

<img width="872" height="627" alt="image" src="https://github.com/user-attachments/assets/fe25e170-2965-42e0-88ac-56f92d0363f0" />


<img width="860" height="613" alt="image" src="https://github.com/user-attachments/assets/adc8efb7-6144-4e3e-a712-12c6557cd58b" />



## Server Console

<img width="1226" height="650" alt="image" src="https://github.com/user-attachments/assets/757cb05e-828c-4702-ba06-c71199e2a0f2" />


## Video Streaming

<img width="1056" height="762" alt="image" src="https://github.com/user-attachments/assets/c33b9b05-41e0-4640-a420-284c484aaa44" />

## Multiple Clients

<img width="970" height="568" alt="image" src="https://github.com/user-attachments/assets/59f4e304-c63d-47c2-8b63-f420584f3ddb" />

---

# Future Improvements

Possible future upgrades include:

- Modern UI redesign
- Authentication system
- Subtitle support
- Internet-wide streaming
- Database media library
- Adaptive bitrate streaming

---

# Author

University Multimedia Assignment Project

Developed in Java using FFmpeg and Socket Programming.
