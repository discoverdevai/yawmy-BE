package com.discoverdev.nms.auth.dto;

/**
 * Returned by forgot-password / resend. {@code token} is the opaque reset token the
 * client keeps and later sends to verify-otp and reset-password. {@code expiresInSeconds}
 * drives the on-screen countdown for the emailed OTP.
 */
public record ForgotPasswordResponse(String token, long expiresInSeconds) {}
