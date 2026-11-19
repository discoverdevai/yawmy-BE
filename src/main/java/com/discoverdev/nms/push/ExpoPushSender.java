package com.discoverdev.nms.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends push notifications through Expo's push service (the apps are Expo apps). Best effort:
 * a failure is logged and never breaks the action that caused it.
 */
@Component
public class ExpoPushSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final boolean enabled;
    private final String url;

    public ExpoPushSender(
        ObjectMapper json,
        @Value("${app.push.enabled:true}") boolean enabled,
        @Value("${app.push.expo-url:https://exp.host/--/api/v2/push/send}") String url
    ) {
        this.json = json;
        this.enabled = enabled;
        this.url = url;
    }

    /** One push per device token, with the default sound. */
    public void send(List<String> deviceTokens, String title, String body, Map<String, Object> data) {
        if (deviceTokens.isEmpty()) return;
        if (!enabled) {
            log.info("Push disabled (app.push.enabled=false): would send to {} device(s)", deviceTokens.size());
            return;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : deviceTokens) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("to", token);
            m.put("title", title);
            m.put("body", body);
            m.put("sound", "default");
            m.put("data", data);
            messages.add(m);
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(messages)))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Push sent to {} device(s): HTTP {}", deviceTokens.size(), res.statusCode());
        } catch (Exception e) {
            log.warn("Push to {} device(s) failed: {}", deviceTokens.size(), e.toString());
        }
    }
}
