package com.tradepilot.preanalysis.repository;

import com.tradepilot.entity.User;
import com.tradepilot.preanalysis.entity.TradingPlaybook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TradingPlaybookRepository extends JpaRepository<TradingPlaybook, Long> {
    Optional<TradingPlaybook> findFirstByUserAndActiveTrueOrderByUpdatedAtDesc(User user);
    List<TradingPlaybook> findByUserOrderByUpdatedAtDesc(User user);
}
