package com.tradepilot.repository;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.PropFirmType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {

    List<TradingAccount> findByUserOrderByCreatedAtDesc(User user);

    List<TradingAccount> findByUserIsNullOrderByCreatedAtDesc();

    Optional<TradingAccount> findByIdAndUser(Long id, User user);

    List<TradingAccount> findByUserAndPropFirmOrderByCreatedAtDesc(User user, PropFirmType propFirm);
}