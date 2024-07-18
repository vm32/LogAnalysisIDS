package alerting;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;

public class AlertingSystem {
    private static final Logger LOGGER = Logger.getLogger(AlertingSystem.class.getName());
    private Properties properties;
    private ExecutorService executorService;

    public AlertingSystem() {
        properties = new Properties();
        executorService = Executors.newFixedThreadPool(2);
        loadConfiguration();
    }

    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("alerting.properties")) {
            if (input == null) {
                LOGGER.log(Level.SEVERE, "Sorry, unable to find alerting.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading configuration", ex);
        }
    }

    public void sendAlert(String alertMessage) {
        // Print to console
        System.out.println("ALERT: " + alertMessage);

        // Send email alert
        if (Boolean.parseBoolean(properties.getProperty("alert.email.enabled", "false"))) {
            executorService.submit(() -> sendEmailAlert(alertMessage));
        }

        // Send SMS alert
        if (Boolean.parseBoolean(properties.getProperty("alert.sms.enabled", "false"))) {
            executorService.submit(() -> sendSmsAlert(alertMessage));
        }

        // Send Slack alert
        if (Boolean.parseBoolean(properties.getProperty("alert.slack.enabled", "false"))) {
            executorService.submit(() -> sendSlackAlert(alertMessage));
        }
    }

    private void sendEmailAlert(String alertMessage) {
        String to = properties.getProperty("alert.email.to");
        String from = properties.getProperty("alert.email.from");
        String host = properties.getProperty("alert.email.smtp.host");
        String subject = properties.getProperty("alert.email.subject", "IDS Alert");

        Properties mailProperties = System.getProperties();
        mailProperties.setProperty("mail.smtp.host", host);

        Session session = Session.getDefaultInstance(mailProperties);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(alertMessage);

            Transport.send(message);
            LOGGER.log(Level.INFO, "Sent email successfully.");
        } catch (MessagingException mex) {
            LOGGER.log(Level.SEVERE, "Failed to send email", mex);
        }
    }

    private void sendSmsAlert(String alertMessage) {
        // Implement SMS alert logic here
        LOGGER.log(Level.INFO, "Sent SMS alert: " + alertMessage);
    }

    private void sendSlackAlert(String alertMessage) {
        // Implement Slack alert logic here
        LOGGER.log(Level.INFO, "Sent Slack alert: " + alertMessage);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        LOGGER.log(Level.INFO, "Alerting system shutdown completed.");
    }
}
