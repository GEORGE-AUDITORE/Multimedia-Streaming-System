package server;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerLogger {
    private static final Logger logger = Logger.getLogger("StreamingServerLogger");

    static{
        try{
            logger.setUseParentHandlers(false);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);

            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            logger.setLevel(Level.INFO);
        }catch (IOException e){
            System.out.println("Logger setup failed: " + e.getMessage());
        }
    }

    public static Logger getLogger(){
        return logger;
    }
    
}
