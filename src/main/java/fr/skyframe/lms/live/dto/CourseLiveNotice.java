package fr.skyframe.lms.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Whether a live quiz is running in a course, so the course page can show a Join button.
 * Sent on /topic/courses/{courseId}/live when a game starts or ends.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseLiveNotice(boolean active, String gameId, String pin, String quizTitle, Long hostId,
                               String hostName) {

    public static CourseLiveNotice none() {
        return new CourseLiveNotice(false, null, null, null, null, null);
    }
}
