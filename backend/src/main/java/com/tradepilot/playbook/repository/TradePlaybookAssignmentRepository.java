package com.tradepilot.playbook.repository;

import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TradePlaybookAssignmentRepository extends JpaRepository<TradePlaybookAssignment, Long> {
    @EntityGraph(attributePaths = {"trade", "setup", "account"})
    List<TradePlaybookAssignment> findBySetup_IdOrderByTrade_TradeDateDescTrade_EntryTimeDescIdDesc(Long setupId);

    @EntityGraph(attributePaths = {"trade", "setup", "account"})
    Optional<TradePlaybookAssignment> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"trade", "setup", "account"})
    Optional<TradePlaybookAssignment> findByTrade_IdAndUser_Id(Long tradeId, Long userId);

    long countBySetup_Id(Long setupId);
    long countByUser_Id(Long userId);
    void deleteBySetup_IdAndTrade_IdAndUser_Id(Long setupId, Long tradeId, Long userId);
}
