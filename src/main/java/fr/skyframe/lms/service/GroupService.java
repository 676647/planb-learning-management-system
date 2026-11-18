package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.Course;
import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.domain.StudentGroup;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.CourseRepository;
import fr.skyframe.lms.repository.EnrollmentRepository;
import fr.skyframe.lms.repository.StudentGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Student groups for group work. Only students of the course can join, and only one group each. */
@Service
@Transactional
public class GroupService {

    private final StudentGroupRepository groups;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UserService userService;

    public GroupService(StudentGroupRepository groups, CourseRepository courses,
                        EnrollmentRepository enrollments, UserService userService) {
        this.groups = groups;
        this.courses = courses;
        this.enrollments = enrollments;
        this.userService = userService;
    }

    public StudentGroup create(Long courseId, String name) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course " + courseId + " not found"));
        StudentGroup group = new StudentGroup();
        group.setCourse(course);
        group.setName(name);
        return groups.save(group);
    }

    @Transactional(readOnly = true)
    public StudentGroup get(Long id) {
        return groups.findById(id)
                .orElseThrow(() -> new NotFoundException("Group " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> listForCourse(Long courseId) {
        return groups.findByCourseId(courseId);
    }

    /** The group a student belongs to in a course, if any. */
    @Transactional(readOnly = true)
    public Optional<StudentGroup> groupOf(Long courseId, Long userId) {
        return groups.findByCourseIdAndMembersId(courseId, userId);
    }

    public StudentGroup addMember(Long groupId, String email) {
        StudentGroup group = get(groupId);
        Long courseId = group.getCourse().getId();
        User user = userService.getByEmail(email);
        if (!enrollments.existsByUserIdAndCourseIdAndRole(user.getId(), courseId, Role.STUDENT)) {
            throw new IllegalArgumentException(email + " is not a student in course " + courseId);
        }
        if (groupOf(courseId, user.getId()).isPresent()) {
            throw new ConflictException(email + " is already in a group in course " + courseId);
        }
        group.getMembers().add(user);
        return group;
    }

    public StudentGroup removeMember(Long groupId, Long userId) {
        StudentGroup group = get(groupId);
        group.getMembers().removeIf(m -> m.getId().equals(userId));
        return group;
    }

    /** Takes the user out of their group in the course, if they are in one (e.g. when they leave the course). */
    public void leaveGroupIn(Long courseId, Long userId) {
        groupOf(courseId, userId).ifPresent(group -> group.getMembers().removeIf(m -> m.getId().equals(userId)));
    }

    /** Called by CourseService when a course is deleted (after its submissions are gone). */
    public void deleteAllOfCourse(Long courseId) {
        for (StudentGroup group : groups.findByCourseId(courseId)) {
            group.getMembers().clear();
            groups.delete(group);
        }
    }
}
