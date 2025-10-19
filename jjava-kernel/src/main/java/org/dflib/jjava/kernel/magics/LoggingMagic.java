package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.kernel.JavaKernel;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Magic command to control Java logging for the kernel.
 * 
 * Usage:
 * %log start [filename] - Start logging to file (default: <kernelname>.log)
 * %log stop - Stop logging and close log file
 * %log on - Resume logging (if paused)
 * %log off - Pause logging (but keep file open)
 * %log state - Show current logging status
 * %log level <LEVEL> - Set logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
 */
public class LoggingMagic implements LineMagic<Void, JavaKernel> {

    Logger log = Logger.getLogger(LoggingMagic.class.getName());

    private static final Logger logger = Logger.getLogger(LoggingMagic.class.getName());
    private static FileHandler fileHandler;
    private static boolean loggingEnabled = false;
    private static boolean loggingPaused = false;
    private static Level currentLevel = Level.INFO;
    private static String currentLogFile = null;

    @Override
    public Void eval(JavaKernel kernel, List<String> args) throws Exception {
        if (args.isEmpty()) {
            System.out.println(getUsage());
            return null;
        }

        String command = args.get(0).toLowerCase();
        
        switch (command) {
            case "start":
                String filename = args.size() > 1 ? args.get(1) : getLogFileName(kernel);
                System.out.println(startLogging(kernel, filename));
                log.info("Logging started. Log file: " + filename + " (Level: " + currentLevel + ")");
                return null;
            case "stop":
                System.out.println(stopLogging());
                return null;
            case "on":
                System.out.println(resumeLogging());
                return null;
            case "off":
                System.out.println(pauseLogging());
                return null;
            case "state":
                System.out.println(getLoggingStatus(kernel));
                return null;
            case "level":
                if (args.size() < 2) {
                    System.out.println("Error: Please specify a logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)");
                    return null;
                }
                System.out.println(setLoggingLevel(args.get(1)));
                return null;
            default:
                System.out.println(getUsage());
                return null;
        }
    }


    private String setLoggingLevel(String levelName) {
        try {
            Level newLevel = Level.parse(levelName.toUpperCase());
            currentLevel = newLevel;
            
            if (fileHandler != null) {
                fileHandler.setLevel(newLevel);
                Logger rootLogger = Logger.getLogger("");
                rootLogger.setLevel(newLevel);
            }
            
            return "Logging level set to: " + newLevel;
        } catch (IllegalArgumentException e) {
            return "Error: Invalid logging level '" + levelName + "'. Valid levels: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST";
        }
    }

    private String getLoggingStatus(JavaKernel kernel) {
        StringBuilder status = new StringBuilder();
        status.append("Logging Status:\n");
        status.append("  Active: ").append(loggingEnabled ? "Yes" : "No").append("\n");
        
        if (loggingEnabled) {
            status.append("  Paused: ").append(loggingPaused ? "Yes" : "No").append("\n");
            status.append("  Log file: ").append(currentLogFile != null ? currentLogFile : getLogFileName(kernel)).append("\n");
        }
        
        status.append("  Level: ").append(currentLevel);
        
        return status.toString();
    }

    private String getLogFileName(JavaKernel kernel) {
        String kernelName = kernel.getName().toLowerCase();
        return kernelName + ".log";
    }

    private String startLogging(JavaKernel kernel, String filename) {
        if (loggingEnabled && !loggingPaused) {
            return "Logging is already active. Log file: " + currentLogFile;
        }

        try {
            currentLogFile = filename;
            fileHandler = new FileHandler(filename, true); // append mode
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(currentLevel);

            // Get root logger and configure it
            Logger rootLogger = Logger.getLogger("");
            
            // Remove all existing handlers (including console handlers)
            Handler[] existingHandlers = rootLogger.getHandlers();
            for (Handler handler : existingHandlers) {
                rootLogger.removeHandler(handler);
            }
            
            // Add only our file handler
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(currentLevel);

            // Also set level for our specific logger
            logger.setLevel(currentLevel);

            loggingEnabled = true;
            loggingPaused = false;
            return "Logging started. Log file: " + filename + " (Level: " + currentLevel + ")";
        } catch (IOException e) {
            return "Error starting logging: " + e.getMessage();
        }
    }

    private String stopLogging() {
        if (!loggingEnabled) {
            return "Logging is not active";
        }

        if (fileHandler != null) {
            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
        }

        loggingEnabled = false;
        loggingPaused = false;
        String logFile = currentLogFile;
        currentLogFile = null;
        return "Logging stopped. Log file was: " + logFile;
    }

    private String pauseLogging() {
        if (!loggingEnabled) {
            return "Logging is not active";
        }
        
        if (loggingPaused) {
            return "Logging is already paused";
        }

        // Temporarily disable the file handler
        if (fileHandler != null) {
            fileHandler.setLevel(Level.OFF);
        }
        
        loggingPaused = true;
        return "Logging paused. Log file: " + currentLogFile;
    }

    private String resumeLogging() {
        if (!loggingEnabled) {
            return "Logging is not active. Use %log start to start logging.";
        }
        
        if (!loggingPaused) {
            return "Logging is already active";
        }

        // Re-enable the file handler
        if (fileHandler != null) {
            fileHandler.setLevel(currentLevel);
        }
        
        loggingPaused = false;
        return "Logging resumed. Log file: " + currentLogFile + " (Level: " + currentLevel + ")";
    }

    private String getUsage() {
        return "Usage: %log <command>\n" +
               "\nCommands:\n" +
               "  start [filename] - Start logging to file (default: <kernelname>.log)\n" +
               "  stop             - Stop logging and close log file\n" +
               "  on               - Resume logging (if paused)\n" +
               "  off              - Pause logging (but keep file open)\n" +
               "  state            - Show current logging status\n" +
               "  level <LEVEL>    - Set logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)\n" +
               "\nExamples:\n" +
               "  %log start                   - Start logging to <kernelname>.log\n" +
               "  %log start myapp.log         - Start logging to myapp.log\n" +
               "  %log off                     - Pause logging\n" +
               "  %log on                      - Resume logging\n" +
               "  %log state                   - Check status\n" +
               "  %log level WARNING           - Set logging level\n" +
               "  %log stop                    - Stop and close log file";
    }

}
