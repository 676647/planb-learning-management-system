package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.FileReference;

/** What clients see of an attached file. The storage key stays internal. */
public record FileResponse(String name, String contentType, Long size) {

    public static FileResponse from(FileReference f) {
        return f == null || f.getStorageKey() == null ? null
                : new FileResponse(f.getOriginalFilename(), f.getContentType(), f.getSize());
    }
}
