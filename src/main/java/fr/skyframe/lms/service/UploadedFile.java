package fr.skyframe.lms.service;

import org.springframework.core.io.InputStreamSource;

/**
 * A file sent by a user, as the service layer sees it (independent of how it arrived over HTTP).
 * Spring's MultipartFile is an InputStreamSource, so controllers can pass it in directly.
 */
public record UploadedFile(String filename, String contentType, long size, InputStreamSource content) {
}
