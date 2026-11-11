package adaptivelearning.module.examimports.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/** Cloudinary-backed storage. Existing UUID keys remain readable from local disk. */
@Service
@ConditionalOnProperty(name = "cloudinary.enabled", havingValue = "true")
public class CloudinaryFileStorage implements FileStorage {
    private final WebClient client;
    private final ObjectMapper mapper;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final LocalFileStorage legacy;

    public CloudinaryFileStorage(WebClient.Builder builder, ObjectMapper mapper,
                                 @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
                                 @Value("${CLOUDINARY_API_KEY:}") String apiKey,
                                 @Value("${CLOUDINARY_API_SECRET:}") String apiSecret,
                                 @Value("${EXAM_ASSET_STORAGE_PATH:./data/exam-assets}") String legacyPath) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_API_KEY va CLOUDINARY_API_SECRET are required");
        }
        this.client = builder.build();
        this.mapper = mapper;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.legacy = new LocalFileStorage(legacyPath);
    }

    @Override
    public StoredFile store(byte[] content, String originalName, String contentType) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("File rong.");
        String resourceType = resourceType(contentType, originalName);
        String publicId = "adaptive-learning/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        MultipartBodyBuilder form = new MultipartBodyBuilder();
        form.part("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return originalName == null ? "upload" : originalName; }
        }).contentType(MediaType.APPLICATION_OCTET_STREAM);
        form.part("api_key", apiKey);
        form.part("timestamp", String.valueOf(timestamp));
        form.part("public_id", publicId);
        form.part("signature", signature);
        JsonNode response = client.post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/" + resourceType + "/upload", cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form.build())
                .retrieve().bodyToMono(JsonNode.class).block();
        if (response == null || response.path("secure_url").asText().isBlank()) {
            throw new IllegalStateException("Cloudinary khong tra ve URL file.");
        }
        String key = "cloudinary|" + resourceType + "|" + publicId + "|" + response.path("secure_url").asText();
        return new StoredFile(key, safeName(originalName), safeContentType(originalName, contentType),
                content.length, sha256(content));
    }

    @Override
    public byte[] read(String key) {
        if (key != null && key.startsWith("cloudinary|")) {
            String[] parts = key.split("\\|", 4);
            if (parts.length == 4) {
                return client.get().uri(parts[3]).retrieve().bodyToMono(byte[].class).block();
            }
        }
        return legacy.read(key);
    }

    @Override
    public void delete(String key) {
        if (key == null || !key.startsWith("cloudinary|")) { legacy.delete(key); return; }
        String[] parts = key.split("\\|", 4);
        if (parts.length != 4) return;
        String resourceType = parts[1], publicId = parts[2];
        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        MultipartBodyBuilder form = new MultipartBodyBuilder();
        form.part("api_key", apiKey); form.part("timestamp", String.valueOf(timestamp));
        form.part("public_id", publicId); form.part("signature", signature);
        client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/{type}/destroy", cloudName, resourceType)
                .contentType(MediaType.MULTIPART_FORM_DATA).bodyValue(form.build()).retrieve()
                .bodyToMono(String.class).block();
    }

    private String resourceType(String contentType, String name) {
        String value = (contentType == null ? "" : contentType).toLowerCase(Locale.ROOT);
        if (value.startsWith("image/")) return "image";
        if (value.startsWith("audio/")) return "video";
        return "raw";
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "upload";
        String normalized = name.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\\"]", "_");
    }

    private String safeContentType(String name, String fallback) {
        String lower = safeName(name).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        return fallback == null || fallback.isBlank() ? "application/octet-stream" : fallback;
    }

    private String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest("SHA-256", bytes)); }
    private String sha1(String value) { return HexFormat.of().formatHex(digest("SHA-1", value.getBytes(StandardCharsets.UTF_8))); }
    private byte[] digest(String algorithm, byte[] bytes) {
        try { return MessageDigest.getInstance(algorithm).digest(bytes); }
        catch (Exception ex) { throw new IllegalStateException("Hash algorithm unavailable", ex); }
    }
}
