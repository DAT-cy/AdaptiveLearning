package adaptivelearning.module.examimports.storage;

public interface FileStorage {
    StoredFile store(byte[] content, String originalName, String contentType);

    byte[] read(String key);

    void delete(String key);
}
