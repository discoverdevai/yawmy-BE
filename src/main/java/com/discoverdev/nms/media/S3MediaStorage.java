package com.discoverdev.nms.media;

import com.discoverdev.nms.config.MediaProperties;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Media stored in S3, keyed exactly as on local disk ("media/{nurseryId}/{uuid}.ext"),
 * so the two backends are interchangeable and a key written by one is readable
 * by the other after a bucket sync.
 *
 * Objects stay private. Everything is streamed back through
 * {@code GET /api/files/media/{id}}, which is where the per-role visibility rules
 * live — handing out presigned URLs instead would move that decision to whoever
 * holds the link.
 */
public class S3MediaStorage implements MediaStorage {

    private final S3Client client;
    private final String bucket;

    public S3MediaStorage(S3Client client, MediaProperties.S3Props props) {
        this.client = client;
        this.bucket = props.bucket();
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType)
        throws IOException {
        requireKey(key);
        try {
            client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build(),
                // The length is known, so the SDK streams straight through
                // instead of buffering the whole file to compute it.
                RequestBody.fromInputStream(content, contentLength)
            );
        } catch (S3Exception e) {
            throw new IOException("S3 upload failed for " + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Resource load(String key) throws IOException {
        requireKey(key);
        try {
            ResponseInputStream<GetObjectResponse> stream = client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
            );
            long length = stream.response().contentLength() != null
                ? stream.response().contentLength()
                : -1L;
            return new S3ObjectResource(stream, key, length);
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (S3Exception e) {
            throw new IOException("S3 read failed for " + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        requireKey(key);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new IOException("S3 delete failed for " + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null || key.isBlank()) return false;
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return false;
        }
    }

    private void requireKey(String key) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IOException("Empty storage key");
        }
    }

    /**
     * Releases the SDK's connection pool. Spring infers this as the destroy
     * method for the bean, so the client shuts down with the context.
     */
    public void close() {
        client.close();
    }

    /**
     * An S3 object as a Spring Resource. The length comes from the GET response,
     * so Spring can set Content-Length without the default InputStreamResource
     * behaviour of draining the stream to measure it — which would leave nothing
     * for the client to read.
     */
    private static final class S3ObjectResource extends InputStreamResource {

        private final String key;
        private final long length;

        private S3ObjectResource(InputStream stream, String key, long length) {
            super(stream);
            this.key = key;
            this.length = length;
        }

        @Override
        public long contentLength() throws IOException {
            return length >= 0 ? length : super.contentLength();
        }

        @Override
        public String getFilename() {
            int slash = key.lastIndexOf('/');
            return slash >= 0 ? key.substring(slash + 1) : key;
        }

        @Override
        public String getDescription() {
            return "S3 object [" + key + "]";
        }
    }
}
