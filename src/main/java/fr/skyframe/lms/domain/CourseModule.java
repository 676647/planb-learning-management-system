package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * A module (section) of a course, e.g. "Week 1: Introduction".
 * Named CourseModule because "Module" clashes with Java's built-in java.lang.Module.
 * Modules are ordered by position within their course and contain ordered content items.
 * A module can require other modules of the same course: the intended semantics is that it
 * only becomes visible to a student once all required modules are completed.
 */
@Entity
@Table(name = "modules")
public class CourseModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    /** Position within the course, starting at 0. */
    @Column(nullable = false)
    private int position;

    /** Modules that must be completed before this one becomes available. */
    @ManyToMany
    @JoinTable(name = "module_dependencies",
            joinColumns = @JoinColumn(name = "module_id"),
            inverseJoinColumns = @JoinColumn(name = "required_module_id"))
    private Set<CourseModule> requires = new HashSet<>();

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Set<CourseModule> getRequires() {
        return requires;
    }

    public void setRequires(Set<CourseModule> requires) {
        this.requires = requires;
    }
}
