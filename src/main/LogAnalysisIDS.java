package main;

import logcollection.LogCollector;
import logparsing.LogParser;
import detection.RuleBasedDetector;
import alerting.AlertingSystem;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogAnalysisIDS {
    private static final Logger LOGGER = Logger.getLogger(LogAnalysisIDS.class.getName());

    public static void main(String[] args) {
        LogCollector logCollector = new LogCollector();
        LogParser logParser = new LogParser();
        RuleBasedDetector detector = new RuleBasedDetector();
        AlertingSystem alertingSystem = new AlertingSystem();

        try {
            List<String> logs = logCollector.collectLogs();
            for (String log : logs) {
                try {
                    Map<String, String> parsedLog = logParser.parseLog(log);
                    List<String> alerts = detector.detectSuspiciousActivity(parsedLog);
                    for (String alert : alerts) {
                        alertingSystem.sendAlert(alert + ": " + parsedLog.get("message"));
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.SEVERE, "Failed to parse log: " + log, e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error collecting logs", e);
        } finally {
            alertingSystem.shutdown();
        }
    }
}
