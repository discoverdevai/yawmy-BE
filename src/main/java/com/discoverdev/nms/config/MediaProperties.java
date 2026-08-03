package com.discoverdev.nms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(LocalProps local, S3Props s3) {

    public record LocalProps(String root) {}

    /**
     * S3 media storage. Only `bucket` decides whether S3 is used at all — see
     * {@code MediaStorageConfig}. Leaving `accessKey`/`secretKey` unset is the
     * normal case on a host with an instance role or a shared credentials file:
     * the SDK's default provider chain finds those on its own.
     *
     * `endpoint` points the same code at any S3-compatible provider — Supabase
     * Storage, Cloudflare R2, Backblaze B2, MinIO — so moving between them is a
     * config change, not a rewrite. Leave it blank for real AWS.
     */
    public record S3Props(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        String endpoint,
        Boolean pathStyle
    ) {

        public boolean configured() {
            return bucket != null && !bucket.isBlank();
        }

        public boolean hasStaticCredentials() {
            return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
        }

        public boolean hasCustomEndpoint() {
            return endpoint != null && !endpoint.isBlank();
        }

        /**
         * Virtual-hosted addressing puts the bucket in the hostname
         * (bucket.s3.amazonaws.com), which only AWS resolves. Every other
         * provider needs the bucket in the path, so a custom endpoint implies
         * path style unless something says otherwise.
         */
        public boolean usePathStyle() {
            return pathStyle != null ? pathStyle : hasCustomEndpoint();
        }
    }
}
