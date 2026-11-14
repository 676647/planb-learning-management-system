package fr.skyframe.lms.domain;


import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    /** BCrypt hash of the password. The plain password is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * Account-wide role. Nullable in the database so the column can be added to an existing
     * users table; a missing value means STUDENT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", length = 20)
    private AccountRole accountRole = AccountRole.STUDENT;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public AccountRole getAccountRole() {
        return accountRole == null ? AccountRole.STUDENT : accountRole;
    }

    public void setAccountRole(AccountRole accountRole) {
        this.accountRole = accountRole;
    }
}
