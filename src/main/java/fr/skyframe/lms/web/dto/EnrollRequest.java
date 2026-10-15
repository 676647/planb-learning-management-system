package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Users are identified by e-mail, so teachers invite students by their address. */
public record EnrollRequest(
        @NotBlank @Email String email,
        @NotNull Role role
) {
}
