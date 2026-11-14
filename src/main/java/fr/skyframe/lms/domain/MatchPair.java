package fr.skyframe.lms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One pair of a matching question, e.g. "HTTP" → "Application layer protocol". */
@Embeddable
public class MatchPair {

    @Column(name = "left_text", length = 1000)
    private String left;

    @Column(name = "right_text", length = 1000)
    private String right;

    protected MatchPair() {
        // for JPA
    }

    public MatchPair(String left, String right) {
        this.left = left;
        this.right = right;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }
}
