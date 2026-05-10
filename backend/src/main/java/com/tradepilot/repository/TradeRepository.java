package com.tradepilot.repository;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {

    List<Trade> findByUserOrderByTradeDateDesc(User user);

    List<Trade> findByUserAndTradeDateBetweenOrderByTradeDateDesc(User user, LocalDate startDate, LocalDate endDate);

    Optional<Trade> findByIdAndUser(Long id, User user);

    boolean existsByExternalTradeKey(String externalTradeKey);

    // ✅ ADD THIS
    void deleteByAccountId(Long accountId);

}
