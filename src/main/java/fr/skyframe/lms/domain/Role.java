package fr.skyframe.lms.domain;

public enum Role {
    TEACHER("teacher"),
    STUDENT("student");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Roles{" +
                "value='" + value + '\'' +
                '}';
    }
}

