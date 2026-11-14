package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.AccountRole;
import jakarta.validation.constraints.NotNull;

public record AccountRoleRequest(@NotNull AccountRole role) {
}
