package com.sayan.rankly.repositories;

import com.sayan.rankly.entities.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findByDomainIdOrderByCreatedAtDesc(Long domainId);

    Optional<Keyword> findByDomainIdAndPhrase(Long domainId, String phrase);

    List<Keyword> findAllByIdInAndDomainId(List<Long> ids, Long domainId);
}
