package com.discoverdev.nms.media;

import com.discoverdev.nms.config.MediaProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Picks the media backend. One bean, chosen once at startup: S3 when a bucket is
 * configured, local disk otherwise.
 *
 * The choice is deliberately explicit rather than two conditionally-registered
 * beans — a missing bucket on a deployed instance should read as one obvious log
 * line, not as a silent fallback nobody notices until the files are gone.
 */
@Configuration
public class MediaStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageConfig.class);

    @Bean
    public MediaStorage mediaStorage(MediaProperties props) {
        MediaProperties.S3Props s3 = props.s3();
        if (s3 == null || !s3.configured()) {
            log.warn("Media storage: LOCAL DISK. Uploads live on the container filesystem and "
                + "are lost on redeploy. Set AWS_S3_BUCKET_NAME to store media in S3.");
            return new LocalDiskMediaStorage(props);
        }
        log.info("Media storage: S3 bucket '{}' in region '{}' at {}.",
            s3.bucket(), s3.region(), s3.hasCustomEndpoint() ? s3.endpoint() : "AWS");
        return new S3MediaStorage(s3Client(s3), s3);
    }

    private S3Client s3Client(MediaProperties.S3Props s3) {
        var builder = S3Client.builder();
        if (s3.region() != null && !s3.region().isBlank()) {
            builder.region(Region.of(s3.region()));
        }
        if (s3.hasCustomEndpoint()) {
            // Supabase Storage, Cloudflare R2, Backblaze B2, MinIO — all speak the
            // S3 protocol, so only the address changes.
            builder.endpointOverride(URI.create(s3.endpoint()));
        }
        builder.forcePathStyle(s3.usePathStyle());
        if (s3.hasStaticCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())));
        } else {
            // Instance role, container role, shared credentials file, env vars —
            // whichever the host provides.
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
