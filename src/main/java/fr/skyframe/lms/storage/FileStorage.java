package fr.skyframe.lms.storage;

import java.io.InputStream;

/**
 * Stores file contents under a key (like a path), e.g. "submissions/12/3f2a…".
 * The rest of the application only uses this interface, so the actual store can be
 * MongoDB GridFS (normal use) or memory (tests) without other code changing.
 */
public interface FileStorage {

    void put(String key, InputStream data, long size, String contentType);

    /** The caller must close the stream. Throws StorageException if the key does not exist. */
    InputStream get(String key);

    void delete(String key);

    boolean exists(String key);
}
