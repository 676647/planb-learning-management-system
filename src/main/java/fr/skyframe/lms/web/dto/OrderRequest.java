package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The ids of all modules (or content items) in their new order. */
public record OrderRequest(@NotNull List<Long> ids) {
}
