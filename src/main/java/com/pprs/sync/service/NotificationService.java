package com.pprs.sync.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.notification.from}")
    private String fromAddress;

    @Value("${app.notification.to}")
    private String[] toAddresses;

    @Value("${app.notification.enabled:true}")
    private boolean notificationsEnabled;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifySuccess(String syncName, int recordCount, LocalDate date) {
        if (!notificationsEnabled) return;

        String subject = String.format("[SYNC SUCCESS] %s — %s", syncName, date);
        String body    = String.format("""
            Sync completed successfully.

            Sync   : %s
            Date   : %s
            Records: %d

            — saral-pprs-data-sync
            """, syncName, date, recordCount);

        send(subject, body);
    }

    public void notifyFailure(String syncName, LocalDate date, Exception e) {
        if (!notificationsEnabled) return;

        String subject = String.format("[SYNC FAILED] %s — %s", syncName, date);
        String body    = String.format("""
            Sync failed. Immediate attention may be required.

            Sync  : %s
            Date  : %s
            Error : %s
            Cause : %s

            Stack Trace:
            %s

            — saral-pprs-data-sync
            """, syncName, date,
            e.getMessage(),
            e.getCause() != null ? e.getCause().getMessage() : "N/A",
            stackTraceToString(e));

        send(subject, body);
    }

    public void notifyPartialSuccess(String syncName, int successCount,
                                     int failCount, LocalDate date) {
        if (!notificationsEnabled) return;

        String subject = String.format("[SYNC PARTIAL] %s — %s", syncName, date);
        String body    = String.format("""
            Sync completed with some failures.

            Sync   : %s
            Date   : %s
            Success: %d records
            Failed : %d records

            Check logs for details on failed records.

            — saral-pprs-data-sync
            """, syncName, date, successCount, failCount);

        send(subject, body);
    }

    public void notifyEmpty(String syncName, LocalDate date) {
        if (!notificationsEnabled) return;
    
        String subject = String.format("[SYNC EMPTY] %s — %s", syncName, date);
        String body    = String.format("""
            Sync completed but no records were returned.
    
            Sync  : %s
            Date  : %s
    
            This may indicate:
            - Market holiday or weekend
            - Source API/file temporarily unavailable
            - Change in source data format
    
            Please verify manually.
    
            — saral-pprs-data-sync
            """, syncName, date);
    
        send(subject, body);
    }

    private void send(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddresses);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Notification sent: {}", subject);
        } catch (Exception e) {
            // Never let notification failure break the sync itself
            //log.error("Password used in mail: {}"message.get);
            log.error("Failed to send notification '{}': {}", subject, e.getMessage());
        }
    }

    private String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        // Truncate to avoid huge emails
        String trace = sw.toString();
        return trace.length() > 2000 ? trace.substring(0, 2000) + "\n... truncated" : trace;
    }
}
