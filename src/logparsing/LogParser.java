package logparsing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
    private List<LogFormat> logFormats;

    public LogParser() {
        logFormats = new ArrayList<>();
        loadLogFormats();
    }

    private void loadLogFormats() {
        // Define different log formats
        logFormats.add(new LogFormat(
            Pattern.compile("^(\\S+) (\\S+) (.+)$"), 
            new String[]{"timestamp", "logLevel", "message"}
        ));
        logFormats.add(new LogFormat(
            Pattern.compile("^(\\S+) (\\S+) \\[(\\S+)] (.+)$"), 
            new String[]{"timestamp", "logLevel", "userId", "message"}
        ));
        // Add more formats as needed
    }

    public Map<String, String> parseLog(String logEntry) {
        for (LogFormat format : logFormats) {
            Matcher matcher = format.getPattern().matcher(logEntry);
            if (matcher.matches()) {
                Map<String, String> parsedLog = new HashMap<>();
                String[] fields = format.getFields();
                for (int i = 0; i < fields.length; i++) {
                    parsedLog.put(fields[i], matcher.group(i + 1));
                }
                parseTimestamp(parsedLog);
                return parsedLog;
            }
        }
        throw new IllegalArgumentException("Log entry does not match any known format: " + logEntry);
    }

    private void parseTimestamp(Map<String, String> parsedLog) {
        String timestamp = parsedLog.get("timestamp");
        // Example timestamp formats
        String[] timestampFormats = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss"};

        for (String format : timestampFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                Date date = sdf.parse(timestamp);
                parsedLog.put("timestamp", sdf.format(date));
                return;
            } catch (ParseException e) {
                // Ignore and try the next format
            }
        }
        throw new IllegalArgumentException("Invalid timestamp format: " + timestamp);
    }

    private static class LogFormat {
        private Pattern pattern;
        private String[] fields;

        public LogFormat(Pattern pattern, String[] fields) {
            this.pattern = pattern;
            this.fields = fields;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String[] getFields() {
            return fields;
        }
    }
}
