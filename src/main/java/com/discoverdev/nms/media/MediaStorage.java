package com.discoverdev.nms.media;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface MediaStorage {

    void store(String key, InputStream content, long contentLength, String contentType) throws IOException;

    Resource load(String key) throws IOException;

    void delete(String key) throws IOException;

    boolean exists(String key);
}
