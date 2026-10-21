package fr.skyframe.lms.web.dto;

import java.time.Instant;

/** Returned on login. The client sends accessToken as "Authorization: Bearer <accessToken>". */
public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

    public static TokenResponse bearer(String token, Instant expiresAt) {
        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
