package com.tradepilot.tradereplay.repository;

import com.tradepilot.tradereplay.entity.TradeReplayInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeReplayInsightRepository extends JpaRepository<TradeReplayInsight, Long> {
    List<TradeReplayInsight> findByReplayIdOrderByDisplayOrderAsc(Long replayId);

    long countByReplayIdIn(List<Long> replayIds);

    void deleteByReplayId(Long replayId);

    long deleteByReplayIdIn(List<Long> replayIds);
}
