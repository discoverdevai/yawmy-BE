package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(long ttlHours, String linkBaseUrl) {}
