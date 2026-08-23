package com.ispautomation.modules.course.service;

import com.ispautomation.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Cloudflare R2 storage via the S3-compatible API.
 * Bucket stays private; playback uses short-lived presigned GET URLs.
 */
@ApplicationScoped
public class R2StorageService {

    private static final Logger LOG = Logger.getLogger(R2StorageService.class);
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/ogg",
            "video/quicktime",
            "application/octet-stream"
    );

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.presentation",
            "application/rtf",
            "text/plain",
            "text/csv",
            "application/octet-stream"
    );

    public enum MediaKind {
        VIDEO,
        DOCUMENT,
        IMAGE
    }

    @ConfigProperty(name = "app.r2.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "app.r2.endpoint")
    Optional<String> endpoint;

    @ConfigProperty(name = "app.r2.access-key-id")
    Optional<String> accessKeyId;

    @ConfigProperty(name = "app.r2.secret-access-key")
    Optional<String> secretAccessKey;

    @ConfigProperty(name = "app.r2.bucket")
    Optional<String> bucket;

    @ConfigProperty(name = "app.r2.region", defaultValue = "auto")
    String region;

    @ConfigProperty(name = "app.r2.playback-ttl-seconds", defaultValue = "900")
    long playbackTtlSeconds;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    void init() {
        if (!isConfigured()) {
            LOG.info("Cloudflare R2 is disabled or not fully configured");
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId.get().trim(),
                secretAccessKey.get().trim()
        );
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        URI endpointUri = URI.create(endpoint.get().trim());
        Region awsRegion = Region.of(region == null || region.isBlank() ? "auto" : region.trim());

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        s3Client = S3Client.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(credentialsProvider)
                .region(awsRegion)
                .serviceConfiguration(s3Config)
                .build();

        s3Presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .credentialsProvider(credentialsProvider)
                .region(awsRegion)
                .serviceConfiguration(s3Config)
                .build();

        LOG.infof("Cloudflare R2 ready (bucket=%s, ttl=%ss)", bucket.get(), playbackTtlSeconds);
    }

    @PreDestroy
    void shutdown() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }

    public boolean isEnabled() {
        return isConfigured() && s3Client != null && s3Presigner != null;
    }

    public void requireEnabled() {
        if (!isEnabled()) {
            throw new BusinessException(503, "Cloudflare R2 storage is not configured.");
        }
    }

    public void putObject(String objectKey, InputStream data, long contentLength, String contentType) {
        putObject(objectKey, data, contentLength, contentType, MediaKind.VIDEO);
    }

    public void putObject(
            String objectKey,
            InputStream data,
            long contentLength,
            String contentType,
            MediaKind kind
    ) {
        requireEnabled();
        String type = normalizeContentType(contentType);
        if (!isAllowedType(type, kind)) {
            throw new BusinessException(400, "Unsupported content type for " + kind.name().toLowerCase(Locale.ROOT)
                    + ": " + type);
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket.get().trim())
                .key(objectKey)
                .contentType(type)
                .contentLength(contentLength)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(data, contentLength));
    }

    public PresignedPlayback presignGet(String objectKey) {
        requireEnabled();
        Duration ttl = Duration.ofSeconds(Math.max(60, playbackTtlSeconds));

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(bucket.get().trim()).key(objectKey))
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return new PresignedPlayback(presigned.url().toString(), Instant.now().plus(ttl));
    }

    public void deleteObject(String objectKey) {
        if (!isEnabled() || objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket.get().trim())
                    .key(objectKey)
                    .build());
        } catch (Exception ex) {
            LOG.warnf(ex, "Failed to delete R2 object %s", objectKey);
        }
    }

    public static String extensionForContentType(String contentType, String filename) {
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            int dot = lower.lastIndexOf('.');
            if (dot > 0 && dot < lower.length() - 1) {
                String ext = lower.substring(dot + 1);
                if (ext.matches("[a-z0-9]{2,5}")) {
                    return ext;
                }
            }
        }
        String type = normalizeContentType(contentType);
        return switch (type) {
            case "video/webm" -> "webm";
            case "video/ogg" -> "ogg";
            case "video/quicktime" -> "mov";
            case "application/pdf" -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.ms-powerpoint" -> "ppt";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                 "application/vnd.openxmlformats-officedocument.presentationml.slideshow" -> "pptx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "application/vnd.oasis.opendocument.text" -> "odt";
            case "application/vnd.oasis.opendocument.presentation" -> "odp";
            case "application/rtf" -> "rtf";
            case "text/plain" -> "txt";
            case "text/csv" -> "csv";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> type.startsWith("video/") ? "mp4" : type.startsWith("image/") ? "jpg" : "bin";
        };
    }

    private boolean isAllowedType(String type, MediaKind kind) {
        if (kind == MediaKind.VIDEO) {
            return ALLOWED_VIDEO_TYPES.contains(type) || type.startsWith("video/");
        }
        if (kind == MediaKind.IMAGE) {
            return ALLOWED_IMAGE_TYPES.contains(type) || type.startsWith("image/");
        }
        return ALLOWED_DOCUMENT_TYPES.contains(type)
                || type.equals("application/pdf")
                || type.contains("presentation")
                || type.contains("powerpoint")
                || type.contains("word")
                || type.contains("excel")
                || type.contains("spreadsheet")
                || type.contains("opendocument")
                || type.startsWith("text/");
    }

    private boolean isConfigured() {
        return enabled
                && endpoint.isPresent() && !endpoint.get().isBlank()
                && accessKeyId.isPresent() && !accessKeyId.get().isBlank()
                && secretAccessKey.isPresent() && !secretAccessKey.get().isBlank()
                && bucket.isPresent() && !bucket.get().isBlank();
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        String trimmed = contentType.trim().toLowerCase(Locale.ROOT);
        int semi = trimmed.indexOf(';');
        return semi >= 0 ? trimmed.substring(0, semi).trim() : trimmed;
    }

    public record PresignedPlayback(String url, Instant expiresAt) {
    }
}
