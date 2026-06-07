package com.tradepilot.tradingdna.repository;

import com.tradepilot.tradingdna.entity.TradingDnaSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TradingDnaSnapshotRepository extends JpaRepository<TradingDnaSnapshot, Long> {
    Optional<TradingDnaSnapshot> findTopByUserIdAndAccountIdAndDaysAndIncludeAllAccountsOrderByUpdatedAtDesc(
            Long userId,
            Long accountId,
            Integer days,
            Boolean includeAllAccounts
    );

    @Query("""
            select s
            from TradingDnaSnapshot s
            where s.userId = :userId
              and ((:accountId is null and s.accountId is null) or s.accountId = :accountId)
              and s.days = :days
              and s.includeAllAccounts = :includeAllAccounts
              and s.dataHash = :dataHash
            order by s.updatedAt desc
            """)
    List<TradingDnaSnapshot> findMatchingSnapshot(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("days") Integer days,
            @Param("includeAllAccounts") Boolean includeAllAccounts,
            @Param("dataHash") String dataHash,
            Pageable pageable
    );

    List<TradingDnaSnapshot> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
