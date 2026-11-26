package com.sayan.rankly.repositories;

import com.sayan.rankly.entities.RankHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankHistoryRepository extends JpaRepository<RankHistory, Long> {

    List<RankHistory> findByKeywordIdOrderByCheckedAtAsc(Long keywordId);
}
