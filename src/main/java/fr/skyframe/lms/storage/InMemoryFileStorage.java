package fr.skyframe.lms.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps files in memory. Used by tests (no MongoDB needed) and when running with
 * STORAGE_TYPE=memory. Everything is lost when the application stops.
 */
public class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    @Override
    public void put(String key, InputStream data, long size, String contentType) {
        try {
            files.put(key, data.readAllBytes());
        } catch (IOException e) {
            throw new StorageException("Could not store file " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        byte[] bytes = files.get(key);
        if (bytes == null) {
            throw new StorageException("No such file " + key, null);
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return files.containsKey(key);
    }

    /** For tests: how many stored files have keys starting with the prefix. */
    public long countWithPrefix(String prefix) {
        return files.keySet().stream().filter(k -> k.startsWith(prefix)).count();
    }
}
