package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code ttlMinutes}: overall reset-session cap (how long after requesting a code
 * the reset may still be completed). {@code otpTtlSeconds}: how long each emailed
 * OTP is valid before it must be resent.
 */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(long ttlMinutes, long otpTtlSeconds) {}
