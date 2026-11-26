package com.sayan.rankly.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "rank_history")
public class RankHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "ranking_url", columnDefinition = "TEXT")
    private String rankingUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "serp_features", columnDefinition = "jsonb")
    private List<String> serpFeatures;

    @Column(nullable = false, length = 20)
    private String source = "MANUAL"; // MANUAL | SCHEDULED

    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = OffsetDateTime.now();
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Keyword getKeyword() {
        return keyword;
    }

    public void setKeyword(Keyword keyword) {
        this.keyword = keyword;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public String getRankingUrl() {
        return rankingUrl;
    }

    public void setRankingUrl(String rankingUrl) {
        this.rankingUrl = rankingUrl;
    }

    public List<String> getSerpFeatures() {
        return serpFeatures;
    }

    public void setSerpFeatures(List<String> serpFeatures) {
        this.serpFeatures = serpFeatures;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(OffsetDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
