package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenTtlMinutes,
    long refreshTokenTtlDays
) {}
