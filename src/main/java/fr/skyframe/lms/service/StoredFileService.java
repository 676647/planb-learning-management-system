package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.FileReference;
import fr.skyframe.lms.domain.Submission;
import fr.skyframe.lms.repository.SubmissionRepository;
import fr.skyframe.lms.storage.FileStorage;
import fr.skyframe.lms.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores uploaded files in the file store (MongoDB GridFS) and keeps it in step with the relational database.
 *
 * PostgreSQL and MongoDB are two separate systems, so one database transaction cannot cover both.
 * We therefore tie file changes to the outcome of the relational database transaction:
 * - a file uploaded during a transaction that is rolled back is deleted again;
 * - files of deleted submissions are only removed after the deletion has been committed.
 */
@Service
public class StoredFileService {

    private static final Logger log = LoggerFactory.getLogger(StoredFileService.class);

    private final FileStorage storage;
    private final SubmissionRepository submissions;
    private final Set<String> allowedExtensions;

    public StoredFileService(FileStorage storage, SubmissionRepository submissions,
                             @Value("${lms.storage.allowed-extensions}") String allowedExtensions) {
        this.storage = storage;
        this.submissions = submissions;
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }

    /** Checks and stores a file in the given folder, and returns the reference to keep in the database. */
    public FileReference store(String folder, UploadedFile file) {
        String name = cleanFilename(file.filename());
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Files of type '." + extension + "' are not allowed. Allowed: "
                    + allowedExtensions.stream().sorted().collect(Collectors.joining(", ")));
        }
        if (file.size() <= 0) {
            throw new IllegalArgumentException("The file is empty");
        }
        // The key is random, so users can neither guess other files' keys nor use "../" tricks
        String key = folder + "/" + UUID.randomUUID();
        String contentType = file.contentType() != null ? file.contentType() : "application/octet-stream";
        try (InputStream in = file.content().getInputStream()) {
            storage.put(key, in, file.size(), contentType);
        } catch (IOException e) {
            throw new StorageException("Could not read the uploaded file", e);
        }
        deleteIfRolledBack(key);
        return new FileReference(key, name, contentType, file.size());
    }

    public InputStream open(FileReference file) {
        return storage.get(file.getStorageKey());
    }

    /** Removes the files of all submissions for a content item, once the deletion is committed. */
    public void deleteFilesOfContent(Long contentId) {
        List<String> keys = submissions.findByContentIdOrderBySubmittedAtDesc(contentId).stream()
                .map(Submission::getFile)
                .filter(f -> f != null && f.getStorageKey() != null)
                .map(FileReference::getStorageKey)
                .toList();
        if (!keys.isEmpty()) {
            afterCommit(() -> keys.forEach(this::deleteQuietly));
        }
    }

    private void deleteIfRolledBack(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(key);
                    }
                }
            });
        }
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /** A failed cleanup must not break the user's request; it only leaves an unused file behind. */
    private void deleteQuietly(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("Could not delete file {} from storage: {}", key, e.getMessage());
        }
    }

    /** Keeps only the last part of the name (browsers on Windows may send full paths). */
    private static String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? "file" : name;
    }
}
