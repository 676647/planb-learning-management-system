package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;

/** teacherId is temporary until security is added. */
public record PublishGradesRequest(@NotNull Long teacherId, boolean published) {
}
