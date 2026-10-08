package com.tradepilot.manualbacktest.repository;

import com.tradepilot.manualbacktest.entity.ManualBacktestTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ManualBacktestTradeRepository extends JpaRepository<ManualBacktestTrade, Long> {
    List<ManualBacktestTrade> findBySessionIdAndUserIdOrderByCreatedAtAsc(Long sessionId, Long userId);
    Optional<ManualBacktestTrade> findFirstBySessionIdAndUserIdAndStatusInOrderByCreatedAtDesc(Long sessionId, Long userId, Collection<ManualBacktestTrade.Status> statuses);
    long countBySessionIdAndStatus(Long sessionId, ManualBacktestTrade.Status status);
}
