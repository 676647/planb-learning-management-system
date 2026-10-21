package fr.skyframe.lms.service;

/** Thrown on a failed login. Mapped to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid e-mail or password");
    }
}
