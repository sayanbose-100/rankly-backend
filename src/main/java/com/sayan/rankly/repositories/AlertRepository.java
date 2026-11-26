package com.sayan.rankly.repositories;

import com.sayan.rankly.entities.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("""
            SELECT a
            FROM Alert a
            WHERE a.user.id = :userId
              AND (:domainId IS NULL OR a.domain.id = :domainId)
              AND (:unreadOnly = false OR a.isRead = false)
            ORDER BY a.createdAt DESC
            """)
    List<Alert> findFilteredAlerts(
            @Param("userId") Long userId,
            @Param("domainId") Long domainId,
            @Param("unreadOnly") boolean unreadOnly
    );

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("""
            UPDATE Alert a
            SET a.isRead = true
            WHERE a.user.id = :userId
              AND a.isRead = false
              AND (:domainId IS NULL OR a.domain.id = :domainId)
            """)
    int markAllAsRead(
            @Param("userId") Long userId,
            @Param("domainId") Long domainId
    );
}
