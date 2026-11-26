package com.sayan.rankly.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "search_batches")
public class SearchBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(name = "total_keywords", nullable = false)
    private Integer totalKeywords;

    @Column(name = "completed_keywords", nullable = false)
    private Integer completedKeywords = 0;

    @Column(name = "failed_keywords", nullable = false)
    private Integer failedKeywords = 0;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Integer getTotalKeywords() {
        return totalKeywords;
    }

    public void setTotalKeywords(Integer totalKeywords) {
        this.totalKeywords = totalKeywords;
    }

    public Integer getCompletedKeywords() {
        return completedKeywords;
    }

    public void setCompletedKeywords(Integer completedKeywords) {
        this.completedKeywords = completedKeywords;
    }

    public Integer getFailedKeywords() {
        return failedKeywords;
    }

    public void setFailedKeywords(Integer failedKeywords) {
        this.failedKeywords = failedKeywords;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
