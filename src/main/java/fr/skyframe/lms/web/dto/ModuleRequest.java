package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ModuleRequest(@NotBlank String title) {
}
