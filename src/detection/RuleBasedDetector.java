package detection;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class RuleBasedDetector {
    private List<Rule> rules;
    private List<ThresholdRule> thresholdRules;
    private Map<String, Integer> eventCounts;

    public RuleBasedDetector() {
        rules = new ArrayList<>();
        thresholdRules = new ArrayList<>();
        eventCounts = new HashMap<>();
        loadRules();
    }

    private void loadRules() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("rules.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find rules.properties");
                return;
            }
            properties.load(input);

            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("rule")) {
                    String[] parts = key.split("\\.");
                    int ruleId = Integer.parseInt(parts[0].substring(4));
                    String attribute = parts[1];
                    String value = properties.getProperty(key);

                    Rule rule = rules.stream()
                            .filter(r -> r.getId() == ruleId)
                            .findFirst()
                            .orElseGet(() -> {
                                Rule newRule = new Rule(ruleId);
                                rules.add(newRule);
                                return newRule;
                            });

                    switch (attribute) {
                        case "logLevel":
                            rule.setLogLevel(value);
                            break;
                        case "messagePattern":
                            rule.setMessagePattern(Pattern.compile(value));
                            break;
                        case "description":
                            rule.setDescription(value);
                            break;
                    }
                } else if (key.startsWith("threshold")) {
                    String[] parts = key.split("\\.");
                    int thresholdId = Integer.parseInt(parts[0].substring(9));
                    String attribute = parts[1];
                    String value = properties.getProperty(key);

                    ThresholdRule thresholdRule = thresholdRules.stream()
                            .filter(r -> r.getId() == thresholdId)
                            .findFirst()
                            .orElseGet(() -> {
                                ThresholdRule newThresholdRule = new ThresholdRule(thresholdId);
                                thresholdRules.add(newThresholdRule);
                                return newThresholdRule;
                            });

                    switch (attribute) {
                        case "logLevel":
                            thresholdRule.setLogLevel(value);
                            break;
                        case "messagePattern":
                            thresholdRule.setMessagePattern(Pattern.compile(value));
                            break;
                        case "description":
                            thresholdRule.setDescription(value);
                            break;
                        case "threshold":
                            thresholdRule.setThreshold(Integer.parseInt(value));
                            break;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<String> detectSuspiciousActivity(Map<String, String> parsedLog) {
        List<String> alerts = new ArrayList<>();

        for (Rule rule : rules) {
            if (rule.matches(parsedLog)) {
                alerts.add(rule.getDescription());
            }
        }

        for (ThresholdRule thresholdRule : thresholdRules) {
            if (thresholdRule.matches(parsedLog)) {
                String key = thresholdRule.getLogLevel() + ":" + thresholdRule.getMessagePattern();
                int count = eventCounts.getOrDefault(key, 0) + 1;
                eventCounts.put(key, count);
                if (count >= thresholdRule.getThreshold()) {
                    alerts.add(thresholdRule.getDescription());
                    eventCounts.put(key, 0);  // Reset the count after alerting
                }
            }
        }

        return alerts.isEmpty() ? Collections.emptyList() : alerts;
    }
}

class Rule {
    private int id;
    private String logLevel;
    private Pattern messagePattern;
    private String description;

    public Rule(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Pattern getMessagePattern() {
        return messagePattern;
    }

    public void setMessagePattern(Pattern messagePattern) {
        this.messagePattern = messagePattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean matches(Map<String, String> parsedLog) {
        return logLevel.equals(parsedLog.get("logLevel")) &&
               messagePattern.matcher(parsedLog.get("message")).find();
    }
}

class ThresholdRule extends Rule {
    private int threshold;

    public ThresholdRule(int id) {
        super(id);
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
