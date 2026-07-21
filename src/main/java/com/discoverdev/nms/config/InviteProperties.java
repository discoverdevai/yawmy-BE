package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-app invite link bases: linkBaseUrl → admin, teacherLinkBaseUrl → teacher,
 * parentLinkBaseUrl → parent.
 */
@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(
    long ttlHours,
    String linkBaseUrl,
    String teacherLinkBaseUrl,
    String parentLinkBaseUrl
) {}
