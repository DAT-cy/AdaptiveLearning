package adaptivelearning.module.examimports.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(@Value("${EXAM_ASSET_STORAGE_PATH:./data/exam-assets}") String storagePath) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(byte[] content, String originalName, String contentType) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("File rỗng.");
        String key = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            Files.write(pathFor(key), content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            String safeName = safeOriginalName(originalName);
            return new StoredFile(key, safeName, safeContentType(safeName),
                    content.length, sha256(content));
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu file đề gốc.", exception);
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(pathFor(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Không tìm thấy file đề gốc.", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(pathFor(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa file đề gốc.", exception);
        }
    }

    private Path pathFor(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Storage key không hợp lệ.");
        }
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Storage key không hợp lệ.");
        return file;
    }

    private String safeOriginalName(String value) {
        if (value == null || value.isBlank()) return "exam-upload";
        String normalized = value.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\\"]", "_");
    }

    private String safeContentType(String fileName) {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text/markdown; charset=UTF-8";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
