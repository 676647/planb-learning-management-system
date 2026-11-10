package fr.skyframe.lms.domain;

/**
 * What a user may do in the system as a whole, independent of any course.
 * (Role is the user's role inside one course: TEACHER or STUDENT.)
 *
 * - STUDENT: every new account. Can take part in courses they are invited to.
 * - TEACHER: made a teacher by an admin. Can also create courses.
 * - ADMIN:   decides who is a teacher or admin. Can also create courses.
 */
public enum AccountRole {
    STUDENT, TEACHER, ADMIN;

    public boolean canCreateCourses() {
        return this == TEACHER || this == ADMIN;
    }
}
