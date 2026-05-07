package server;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerStats {

    private static final AtomicInteger activeClients = new AtomicInteger(0);
    private static final AtomicInteger totalClientsServed = new AtomicInteger(0);
    private static final AtomicInteger tcpStreams = new AtomicInteger(0);
    private static final AtomicInteger udpStreams = new AtomicInteger(0);
    private static final AtomicInteger rtpStreams = new AtomicInteger(0);

    public static void clientConnected(){
        activeClients.incrementAndGet();
        totalClientsServed.incrementAndGet();
    }

    public static void clientDisconnected(){
        activeClients.decrementAndGet();
    }

    public static void protocolUsed(String protocol){
        switch (protocol) {
            case "TCP":
                tcpStreams.incrementAndGet();
                break;
            case "UDP":
                udpStreams.incrementAndGet();
                break;
            case "RTP/UDP":
                rtpStreams.incrementAndGet();
                break;
            default:
                break;
        }
    }
    
    public static int getActiveClients(){
        return activeClients.get();

    }

    public static int getTotalClientsServed(){
        return totalClientsServed.get();
    }

    public static int getTcpStreams(){
        return tcpStreams.get();
    }

    public static int getUdpStreams(){
        return udpStreams.get();
    }

    public static int getRtpStreams(){
        return rtpStreams.get();
    }
}
