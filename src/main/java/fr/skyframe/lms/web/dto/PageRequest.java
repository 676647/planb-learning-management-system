package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Page;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PageRequest(
        @NotBlank String title,
        String body,
        Instant visibleFrom,
        Instant visibleUntil
) {
    public Page toEntity() {
        Page page = new Page();
        page.setTitle(title);
        page.setBody(body);
        page.setVisibleFrom(visibleFrom);
        page.setVisibleUntil(visibleUntil);
        return page;
    }
}
