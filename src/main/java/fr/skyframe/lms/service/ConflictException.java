package fr.skyframe.lms.service;

/** Thrown when an action clashes with existing data (e.g. duplicate e-mail). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
