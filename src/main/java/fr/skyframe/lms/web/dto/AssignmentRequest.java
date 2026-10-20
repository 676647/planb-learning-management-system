package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Assignment;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record AssignmentRequest(
        @NotBlank String title,
        String description,
        Instant visibleFrom,
        Instant visibleUntil,
        Instant deadline,
        Boolean lateSubmissionsAllowed,
        Boolean groupWork
) {
    public Assignment toEntity() {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setVisibleFrom(visibleFrom);
        assignment.setVisibleUntil(visibleUntil);
        assignment.setDeadline(deadline);
        if (lateSubmissionsAllowed != null) assignment.setLateSubmissionsAllowed(lateSubmissionsAllowed);
        if (groupWork != null) assignment.setGroupWork(groupWork);
        return assignment;
    }
}
