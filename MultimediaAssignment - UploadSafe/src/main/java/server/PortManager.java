package server;

import java.util.concurrent.atomic.AtomicInteger;

public class PortManager {
    private static final AtomicInteger nextBasePort = new AtomicInteger(7000);
   
    public static int allocateBasePort(){
        return nextBasePort.getAndAdd(10);
    }
    
}
