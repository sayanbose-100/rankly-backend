package com.sayan.rankly.repositories;

import com.sayan.rankly.entities.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    List<Domain> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);

    Optional<Domain> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDomainNameAndTargetCountryAndTargetDevice(
            Long userId,
            String domainName,
            String targetCountry,
            String targetDevice
    );
}
