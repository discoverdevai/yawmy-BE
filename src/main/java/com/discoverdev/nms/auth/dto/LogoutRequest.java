package com.discoverdev.nms.auth.dto;

/**
 * Optional body for POST /api/auth/logout. Supplying the device's refreshToken
 * signs out only that device; omitting it revokes every session for the user.
 */
public record LogoutRequest(String refreshToken) {}
