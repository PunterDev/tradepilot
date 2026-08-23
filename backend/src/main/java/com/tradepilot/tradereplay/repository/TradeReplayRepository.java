package com.tradepilot.tradereplay.repository;

import com.tradepilot.tradereplay.entity.TradeReplay;
import com.tradepilot.tradereplay.entity.TradeReplayStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TradeReplayRepository extends JpaRepository<TradeReplay, Long> {
    @EntityGraph(attributePaths = {"trade", "trade.account", "user"})
    Optional<TradeReplay> findByUserIdAndTradeId(Long userId, Long tradeId);

    boolean existsByUserIdAndTradeId(Long userId, Long tradeId);

    List<TradeReplay> findByUserIdAndTrade_IdIn(Long userId, List<Long> tradeIds);

    long countByUserIdAndTrade_IdIn(Long userId, List<Long> tradeIds);

    long deleteByUserIdAndTrade_IdIn(Long userId, List<Long> tradeIds);

    @EntityGraph(attributePaths = {"trade", "trade.account", "user"})
    @Query("""
            select r from TradeReplay r
            where r.status in :statuses
              and r.eligibleAt <= :now
              and (r.nextRetryAt is null or r.nextRetryAt <= :now)
            order by r.eligibleAt asc, r.id asc
            """)
    List<TradeReplay> findRunnable(
            @Param("statuses") List<TradeReplayStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );
}
