package com.tradepilot.repository;

import com.tradepilot.entity.DailyDisciplineScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyDisciplineScoreRepository extends JpaRepository<DailyDisciplineScore, Long> {
    Optional<DailyDisciplineScore> findByUserIdAndTradeDateAndAccountId(Long userId, LocalDate tradeDate, Long accountId);

    Optional<DailyDisciplineScore> findTopByUserIdAndTradeDateBeforeAndAccountIdOrderByTradeDateDesc(Long userId, LocalDate tradeDate, Long accountId);

    void deleteByUserId(Long userId);
}
