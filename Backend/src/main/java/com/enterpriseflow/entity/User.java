package com.enterpriseflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 16)
    private MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public static User create(String name, String email, String passwordHash, UserRole role, Company company, MembershipStatus membershipStatus) {
        User user = new User();
        user.name = Objects.requireNonNull(name, "name must not be null");
        user.email = Objects.requireNonNull(email, "email must not be null");
        user.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        user.role = Objects.requireNonNull(role, "role must not be null");
        user.company = company;
        user.membershipStatus = Objects.requireNonNull(membershipStatus, "membershipStatus must not be null");
        return user;
    }
    /** Compatibility factory for existing tests and pre-company callers. */
    public static User create(String name, String email, String passwordHash, UserRole role) {
        return create(name, email, passwordHash, role, null, MembershipStatus.ACTIVE);
    }

    public void updateName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public void updateRole(UserRole role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }
    public void updateMembershipStatus(MembershipStatus status) { this.membershipStatus = Objects.requireNonNull(status); }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }
    public Company getCompany() { return company; }
    public MembershipStatus getMembershipStatus() { return membershipStatus; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
