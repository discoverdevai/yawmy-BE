package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(LocalProps local) {

    public record LocalProps(String root) {}
}
