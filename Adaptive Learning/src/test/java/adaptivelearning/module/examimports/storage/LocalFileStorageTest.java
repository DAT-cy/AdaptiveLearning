package adaptivelearning.module.examimports.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {
    @TempDir Path directory;

    @Test
    void storesFileWithSafeKeyAndReturnsItsChecksum() {
        FileStorage storage = new LocalFileStorage(directory.toString());
        byte[] content = "%PDF-test".getBytes(StandardCharsets.UTF_8);

        StoredFile stored = storage.store(content, "..\\source.pdf", "text/html");

        assertEquals("source.pdf", stored.originalName());
        assertEquals("application/pdf", stored.contentType());
        assertEquals(64, stored.sha256().length());
        assertArrayEquals(content, storage.read(stored.key()));
        storage.delete(stored.key());
        assertThrows(IllegalStateException.class, () -> storage.read(stored.key()));
    }

    @Test
    void rejectsPathTraversalKeys() {
        FileStorage storage = new LocalFileStorage(directory.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.read("../../outside"));
    }

    @Test
    void storesAudioWithCanonicalContentType() {
        FileStorage storage = new LocalFileStorage(directory.toString());

        StoredFile stored = storage.store(new byte[] {1, 2, 3}, "listening.mp3", "application/octet-stream");

        assertEquals("audio/mpeg", stored.contentType());
    }
}
