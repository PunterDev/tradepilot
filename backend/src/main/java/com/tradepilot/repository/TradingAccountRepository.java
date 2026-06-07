package com.tradepilot.repository;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.PropFirmType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {

    List<TradingAccount> findByUserOrderByCreatedAtDesc(User user);

    long countByUser(User user);

    Optional<TradingAccount> findByIdAndUser(Long id, User user);

    Optional<TradingAccount> findByIdAndUser_Id(Long id, Long userId);

    List<TradingAccount> findByUserAndPropFirmOrderByCreatedAtDesc(User user, PropFirmType propFirm);

    void deleteByUser(User user);

    @Query("""
            select a.user.id, count(a)
            from TradingAccount a
            where a.user.id in :userIds
            group by a.user.id
            """)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);
}
