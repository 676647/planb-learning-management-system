package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A group of students within one course. For group work, a group hands in a single
 * submission on behalf of all its members, and the grade applies to every member.
 * A student can be in at most one group per course.
 */
@Entity
@Table(name = "student_groups")
public class StudentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    public boolean hasMember(Long userId) {
        return members.stream().anyMatch(m -> m.getId().equals(userId));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }
}
