package fr.skyframe.lms.storage;

/** The file store could not be reached or failed. Mapped to HTTP 503. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
