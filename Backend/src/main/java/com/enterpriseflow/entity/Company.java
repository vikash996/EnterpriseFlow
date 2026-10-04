package com.enterpriseflow.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 160, unique = true) private String name;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected Company() {}
    public Company(String name) { this.name = name; }
    @PrePersist void created() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
