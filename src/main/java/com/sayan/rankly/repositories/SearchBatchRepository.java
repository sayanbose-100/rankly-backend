package com.sayan.rankly.repositories;

import com.sayan.rankly.entities.SearchBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SearchBatchRepository extends JpaRepository<SearchBatch, Long> {

    Optional<SearchBatch> findByIdAndUserId(Long id, Long userId);

    List<SearchBatch> findByDomainIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long domainId,
            Long userId,
            String status
    );

    @Query("""
            SELECT COALESCE(SUM(sb.completedKeywords), 0)
            FROM SearchBatch sb
            WHERE sb.user.id = :userId
              AND sb.createdAt >= :startOfDay
            """)
    int sumCompletedKeywordsSince(
            @Param("userId") Long userId,
            @Param("startOfDay") OffsetDateTime startOfDay
    );
}
