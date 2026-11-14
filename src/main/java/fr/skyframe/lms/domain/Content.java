package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A content item inside a module: a page, quiz, coding exercise or assignment.
 *
 * All subclasses are stored in one table ("contents") with a "content_type" column telling
 * which subclass a row is (single-table inheritance). This keeps queries simple: listing a
 * module's contents in order is a single query, regardless of their types.
 */
@Entity
@Table(name = "contents")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "content_type")
public abstract class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false)
    private String title;

    /** Position within the module, starting at 0. */
    @Column(nullable = false)
    private int position;

    /** Optional visibility window: hidden from students before visibleFrom and from visibleUntil on. */
    private Instant visibleFrom;
    private Instant visibleUntil;

    public abstract ContentType getType();

    /** True if students may see this item at the given time. A missing bound means "no limit". */
    public boolean isVisibleAt(Instant time) {
        boolean started = visibleFrom == null || !time.isBefore(visibleFrom);
        boolean notEnded = visibleUntil == null || time.isBefore(visibleUntil);
        return started && notEnded;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseModule getModule() {
        return module;
    }

    public void setModule(CourseModule module) {
        this.module = module;
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

    public Instant getVisibleFrom() {
        return visibleFrom;
    }

    public void setVisibleFrom(Instant visibleFrom) {
        this.visibleFrom = visibleFrom;
    }

    public Instant getVisibleUntil() {
        return visibleUntil;
    }

    public void setVisibleUntil(Instant visibleUntil) {
        this.visibleUntil = visibleUntil;
    }
}
