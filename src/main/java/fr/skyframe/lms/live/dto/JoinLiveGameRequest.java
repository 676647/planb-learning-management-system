package fr.skyframe.lms.live.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinLiveGameRequest(@NotBlank String pin) {
}
