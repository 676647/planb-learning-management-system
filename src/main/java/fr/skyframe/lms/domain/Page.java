package fr.skyframe.lms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A static page with course material. The body is stored as Markdown/HTML text,
 * so it can embed images and videos by URL.
 */
@Entity
@DiscriminatorValue("PAGE")
public class Page extends Content {

    /** Up to 100,000 characters; a plain long text column works on both H2 and PostgreSQL. */
    @Column(name = "body", length = 100_000)
    private String body;

    @Override
    public ContentType getType() {
        return ContentType.PAGE;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
