package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sender identity for outgoing mail. SMTP host/credentials live under {@code spring.mail.*}
 * (env vars MAIL_USERNAME / MAIL_PASSWORD); {@code from} is the verified Brevo sender.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from, String fromName) {}