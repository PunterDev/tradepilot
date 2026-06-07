package com.tradepilot.repository;

import com.tradepilot.entity.TradeRuleViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TradeRuleViolationRepository extends JpaRepository<TradeRuleViolation, Long> {
    List<TradeRuleViolation> findByUserIdAndTradeIdOrderByCreatedAtDesc(Long userId, Long tradeId);

    List<TradeRuleViolation> findByUserIdAndTradeDateOrderByCreatedAtDesc(Long userId, LocalDate tradeDate);

    void deleteByUserIdAndTradeIdAndSource(Long userId, Long tradeId, String source);

    void deleteByUserId(Long userId);

    @Query("""
            select v from TradeRuleViolation v
            where v.userId = :userId
              and v.tradeDate between :startDate and :endDate
              and (:accountId is null or v.tradeId in (
                  select t.id from Trade t where t.user.id = :userId and t.account.id = :accountId
              ))
            order by v.createdAt desc
            """)
    List<TradeRuleViolation> findForTradingDna(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
