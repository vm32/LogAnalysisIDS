package logcollection;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LogCollector {
    private static final Logger LOGGER = Logger.getLogger(LogCollector.class.getName());
    private List<String> filePaths;
    private List<String> directories;
    private List<String> networkSources;
    private List<Pattern> filePatterns;
    private ExecutorService executorService;
    private Properties properties;

    public LogCollector() {
        filePaths = new ArrayList<>();
        directories = new ArrayList<>();
        networkSources = new ArrayList<>();
        filePatterns = new ArrayList<>();
        properties = new Properties();
        executorService = Executors.newFixedThreadPool(5);
        loadConfiguration();
    }

    private void loadConfiguration() {
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
            for (String pattern : properties.getProperty("log.filePatterns", "").split(",")) {
                filePatterns.add(Pattern.compile(pattern.trim()));
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading configuration", ex);
        }
    }

    public List<String> collectLogs() {
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        List<Runnable> tasks = new ArrayList<>();
        for (String filePath : filePaths) {
            tasks.add(() -> collectLogsFromFile(filePath, logs));
        }
        for (String directory : directories) {
            tasks.add(() -> collectLogsFromDirectory(directory, logs));
        }
        for (String networkSource : networkSources) {
            tasks.add(() -> collectLogsFromNetworkSource(networkSource, logs));
        }

        tasks.forEach(executorService::submit);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            // Wait for all tasks to complete
        }

        return logs;
    }

    private void collectLogsFromFile(String filePath, List<String> logs) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                logs.add(line);
            }
            LOGGER.log(Level.INFO, "Collected logs from file: " + filePath);
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
        File[] files = directory.listFiles((dir, name) -> filePatterns.stream().anyMatch(pattern -> pattern.matcher(name).matches()));
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

        int maxRetries = Integer.parseInt(properties.getProperty("log.networkSources.maxRetries", "3"));
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Socket socket = new Socket(host, port);
                 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    logs.add(line);
                }
                LOGGER.log(Level.INFO, "Collected logs from network source: " + networkSource);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading from network source: " + networkSource + ", attempt " + attempt + " of " + maxRetries, e);
                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to collect logs from network source after " + maxRetries + " attempts: " + networkSource, e);
                }
            }
        }
    }
}
