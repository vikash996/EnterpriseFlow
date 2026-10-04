package com.enterpriseflow.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "join_requests", uniqueConstraints = @UniqueConstraint(name = "uq_join_request_user_company", columnNames = {"user_id", "company_id"}))
public class JoinRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private MembershipStatus status = MembershipStatus.PENDING;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected JoinRequest() {}
    public JoinRequest(User user, Company company) { this.user = user; this.company = company; }
    public void decide(MembershipStatus status) { this.status = status; }
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public User getUser() { return user; } public Company getCompany() { return company; }
    public MembershipStatus getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
}
