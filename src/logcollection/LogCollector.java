package logcollection;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogCollector {
    private static final Logger LOGGER = Logger.getLogger(LogCollector.class.getName());
    private List<String> filePaths;
    private List<String> directories;
    private List<String> networkSources;

    public LogCollector() {
        filePaths = new ArrayList<>();
        directories = new ArrayList<>();
        networkSources = new ArrayList<>();
        loadConfiguration();
    }

    private void loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("logcollector.properties")) {
            if (input == null) {
                LOGGER.log(Level.SEVERE, "Sorry, unable to find logcollector.properties");
                return;
            }
            properties.load(input);

            for (String filePath : properties.getProperty("log.files", "").split(",")) {
                filePaths.add(filePath.trim());
            }
            for (String directory : properties.getProperty("log.directories", "").split(",")) {
                directories.add(directory.trim());
            }
            for (String networkSource : properties.getProperty("log.networkSources", "").split(",")) {
                networkSources.add(networkSource.trim());
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading configuration", ex);
        }
    }

    public List<String> collectLogs() {
        List<String> logs = new ArrayList<>();
        for (String filePath : filePaths) {
            collectLogsFromFile(filePath, logs);
        }
        for (String directory : directories) {
            collectLogsFromDirectory(directory, logs);
        }
        for (String networkSource : networkSources) {
            collectLogsFromNetworkSource(networkSource, logs);
        }
        return logs;
    }

    private void collectLogsFromFile(String filePath, List<String> logs) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading log file: " + filePath, e);
        }
    }

    private void collectLogsFromDirectory(String directoryPath, List<String> logs) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            LOGGER.log(Level.SEVERE, "Not a directory: " + directoryPath);
            return;
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".log"));
        if (files != null) {
            for (File file : files) {
                collectLogsFromFile(file.getAbsolutePath(), logs);
            }
        }
    }

    private void collectLogsFromNetworkSource(String networkSource, List<String> logs) {
        String[] parts = networkSource.split(":");
        if (parts.length != 2) {
            LOGGER.log(Level.SEVERE, "Invalid network source: " + networkSource);
            return;
        }
        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid port in network source: " + networkSource, e);
            return;
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading from network source: " + networkSource, e);
        }
    }
}
