package fr.skyframe.lms.service;

/** Thrown when a user lacks permission for an action (e.g. a student trying to grade). Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
