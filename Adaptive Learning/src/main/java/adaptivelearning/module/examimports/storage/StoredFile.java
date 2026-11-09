package adaptivelearning.module.examimports.storage;

public record StoredFile(String key, String originalName, String contentType, long size, String sha256) {}
