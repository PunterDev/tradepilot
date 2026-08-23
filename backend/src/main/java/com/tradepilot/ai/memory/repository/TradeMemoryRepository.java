package com.tradepilot.ai.memory.repository;

import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.entity.TradeMemory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradeMemoryRepository extends JpaRepository<TradeMemory, UUID> {
    Optional<TradeMemory> findByTradeIdAndUserId(Long tradeId, Long userId);
    Optional<TradeMemory> findByTradeId(Long tradeId);
    List<TradeMemory> findByUserIdAndTradeIdIn(Long userId, List<Long> tradeIds);
    List<TradeMemory> findByUserId(Long userId);
    long countByUserIdAndEmbeddingStatus(Long userId, EmbeddingStatus status);
    long countByEmbeddingStatus(EmbeddingStatus status);
    List<TradeMemory> findByUserIdAndEmbeddingStatusOrderByUpdatedAtAsc(Long userId, EmbeddingStatus status, Pageable pageable);
    List<TradeMemory> findByEmbeddingStatusAndNextRetryAtLessThanEqualOrderByUpdatedAtAsc(EmbeddingStatus status, OffsetDateTime nextRetryAt, Pageable pageable);
    void deleteByTradeIdAndUserId(Long tradeId, Long userId);
    void deleteByUserId(Long userId);

    @Query("select min(m.updatedAt) from TradeMemory m where m.embeddingStatus = :status")
    OffsetDateTime oldestByStatus(@Param("status") EmbeddingStatus status);

    @Query("select max(m.embeddedAt) from TradeMemory m where m.embeddingStatus = :status")
    OffsetDateTime latestEmbeddedAtByStatus(@Param("status") EmbeddingStatus status);
}
