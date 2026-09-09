package com.tradepilot.repository;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {

    @Override
    @EntityGraph(attributePaths = {"account"})
    Page<Trade> findAll(Specification<Trade> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserOrderByTradeDateDesc(User user);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserOrderByTradeDateAscEntryTimeAscIdAsc(User user);

    long countByUser(User user);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserAndExternalTradeKeyStartingWithOrderByExternalTradeKeyAsc(User user, String externalTradeKeyPrefix);

    long countByUserAndExternalTradeKeyStartingWith(User user, String externalTradeKeyPrefix);

    @Query("""
            select t.user.id, count(t)
            from Trade t
            where t.user.id in :userIds
            group by t.user.id
            """)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);

    @Query("""
            select t.account.id, count(t)
            from Trade t
            where t.user = :user
              and t.account is not null
            group by t.account.id
            order by count(t) desc, t.account.id asc
            """)
    List<Object[]> countByAccountForUser(@Param("user") User user);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserAndTradeDateBetweenOrderByTradeDateDesc(User user, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserAndAccount_IdAndTradeDateOrderByEntryTimeAscIdAsc(User user, Long accountId, LocalDate tradeDate);

    @EntityGraph(attributePaths = {"account"})
    @Query("""
            select t from Trade t
            where t.user = :user
              and (:accountId is null or t.account.id = :accountId)
              and (:tradeDate is null or t.tradeDate = :tradeDate)
              and t.externalTradeKey is not null
            order by t.tradeDate asc, t.entryTime asc, t.id asc
            """)
    List<Trade> findImportedTradesForDirectionRepair(
            @Param("user") User user,
            @Param("accountId") Long accountId,
            @Param("tradeDate") LocalDate tradeDate
    );

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserAndAccount_IdAndTradeDateBetweenOrderByTradeDateDesc(
            User user,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUser_IdAndAccount_IdAndTradeDateBetweenOrderByTradeDateAscEntryTimeAscIdAsc(
            Long userId,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(User user, Long accountId);

    @Query("""
            select count(t), max(t.updatedAt), coalesce(sum(t.id), 0), coalesce(sum(t.pnlAmount), 0)
            from Trade t
            where t.user.id = :userId
              and (:accountId is null or t.account.id = :accountId)
              and t.tradeDate between :startDate and :endDate
            """)
    Object tradingDnaFingerprint(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @EntityGraph(attributePaths = {"account"})
    Optional<Trade> findByIdAndUser(Long id, User user);

    @EntityGraph(attributePaths = {"account"})
    Optional<Trade> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUser_IdAndIdIn(Long userId, List<Long> ids);

    boolean existsByUserAndAccount_IdAndExternalTradeKey(User user, Long accountId, String externalTradeKey);

    @Query("""
            select t.externalTradeKey
            from Trade t
            where t.user = :user
              and t.account.id = :accountId
              and t.externalTradeKey in :externalTradeKeys
            """)
    List<String> findExistingExternalTradeKeys(
            @Param("user") User user,
            @Param("accountId") Long accountId,
            @Param("externalTradeKeys") List<String> externalTradeKeys
    );

    void deleteByAccountIdAndUser(Long accountId, User user);

    void deleteByUser(User user);

    @EntityGraph(attributePaths = {"account"})
    List<Trade> findByUser_IdAndAccount_IdAndTradeDateBetween(
            Long userId,
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            select max(t.tradeDate) from Trade t
            where t.user = :user and t.account.id = :accountId and t.tradeDate < :tradeDate
            """)
    LocalDate findPreviousTradingDate(
            @Param("user") User user,
            @Param("accountId") Long accountId,
            @Param("tradeDate") LocalDate tradeDate
    );

    @Query("""
            select min(t.tradeDate) from Trade t
            where t.user = :user and t.account.id = :accountId and t.tradeDate > :tradeDate
            """)
    LocalDate findNextTradingDate(
            @Param("user") User user,
            @Param("accountId") Long accountId,
            @Param("tradeDate") LocalDate tradeDate
    );

    @Query("""
            select t.id from Trade t
            where t.user = :user
              and (
                t.tradeDate < :tradeDate
                or (t.tradeDate = :tradeDate and t.entryTime < :entryTime)
                or (t.tradeDate = :tradeDate and t.entryTime = :entryTime and t.id < :tradeId)
              )
            order by t.tradeDate desc, t.entryTime desc, t.id desc
            """)
    List<Long> findPreviousTradeId(
            @Param("user") User user,
            @Param("tradeId") Long tradeId,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("entryTime") LocalDateTime entryTime,
            Pageable pageable
    );

    @Query("""
            select t.id from Trade t
            where t.user = :user
              and (
                t.tradeDate > :tradeDate
                or (t.tradeDate = :tradeDate and t.entryTime > :entryTime)
                or (t.tradeDate = :tradeDate and t.entryTime = :entryTime and t.id > :tradeId)
              )
            order by t.tradeDate asc, t.entryTime asc, t.id asc
            """)
    List<Long> findNextTradeId(
            @Param("user") User user,
            @Param("tradeId") Long tradeId,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("entryTime") LocalDateTime entryTime,
            Pageable pageable
    );

    @Query(value = """
            select
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0
                     else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*))
                end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            """, nativeQuery = true)
    Object aggregateSummary(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            select
                coalesce(cast(:groupBy as text), '') as ignored_group_by,
                case
                    when :groupBy = 'setup' then coalesce(t.setup, 'Unspecified')
                    when :groupBy = 'session' then coalesce(t.session, 'Unspecified')
                    when :groupBy = 'symbol' then coalesce(t.symbol, 'Unspecified')
                    when :groupBy = 'weekday' then cast(extract(isodow from t.trade_date) as text)
                    else 'Unspecified'
                end as label,
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0
                     else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*))
                end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by label
            order by total_pnl desc
            """, nativeQuery = true)
    List<Object[]> aggregateByDimension(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("groupBy") String groupBy
    );

    @Query(value = """
            select coalesce(t.setup, 'Unspecified') as label,
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0 else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*)) end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by t.setup
            order by total_pnl desc
            """, nativeQuery = true)
    List<Object[]> aggregateBySetup(@Param("userId") Long userId, @Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select coalesce(t.session, 'Unspecified') as label,
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0 else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*)) end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by t.session
            order by total_pnl desc
            """, nativeQuery = true)
    List<Object[]> aggregateBySession(@Param("userId") Long userId, @Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select coalesce(t.symbol, 'Unspecified') as label,
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0 else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*)) end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by t.symbol
            order by total_pnl desc
            """, nativeQuery = true)
    List<Object[]> aggregateBySymbol(@Param("userId") Long userId, @Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select cast(extract(isodow from t.trade_date) as text) as label,
                count(*) as total_trades,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as wins,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losses,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakevens,
                coalesce(sum(t.pnl_amount), 0) as total_pnl,
                avg(case
                    when coalesce(t.risk_amount, a.default_risk_amount) > 0
                    then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                    else null
                end) as average_r,
                case when count(*) = 0 then 0 else (sum(case when t.outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*)) end as win_rate
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by label
            order by label asc
            """, nativeQuery = true)
    List<Object[]> aggregateByWeekday(@Param("userId") Long userId, @Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select
                t.trade_date as date,
                count(*) as trade_count,
                coalesce(sum(t.pnl_amount), 0) as net_pnl,
                case
                    when sum(case when coalesce(t.risk_amount, a.default_risk_amount) > 0 then 1 else 0 end) > 0
                    then sum(case
                        when coalesce(t.risk_amount, a.default_risk_amount) > 0
                        then t.pnl_amount / coalesce(t.risk_amount, a.default_risk_amount)
                        else 0
                    end)
                    else null
                end as total_r,
                sum(case when t.outcome = 'WIN' then 1 else 0 end) as winning_trades,
                sum(case when t.outcome = 'LOSS' then 1 else 0 end) as losing_trades,
                sum(case when t.outcome = 'BE' then 1 else 0 end) as breakeven_trades
            from trades t
            left join trading_accounts a on a.id = t.account_id
            where t.user_id = :userId
              and (:accountId is null or t.account_id = :accountId)
              and (:startDate is null or t.trade_date >= :startDate)
              and (:endDate is null or t.trade_date <= :endDate)
            group by t.trade_date
            order by t.trade_date asc
            """, nativeQuery = true)
    List<Object[]> aggregateDailyPnl(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select
                t.tradeDate as tradeDate,
                t.entryTime as entryTime,
                t.session as session,
                t.outcome as outcome,
                t.pnlAmount as pnlAmount
            from Trade t
            where t.user.id = :userId
              and (:accountId is null or t.account.id = :accountId)
            """)
    List<HeatmapTradeProjection> findHeatmapTrades(@Param("userId") Long userId, @Param("accountId") Long accountId);

    interface HeatmapTradeProjection {
        LocalDate getTradeDate();
        LocalDateTime getEntryTime();
        com.tradepilot.enums.SessionType getSession();
        com.tradepilot.enums.OutcomeType getOutcome();
        java.math.BigDecimal getPnlAmount();
    }

    @Query(value = """
            select
                case
                    when extract(isodow from trade_date) = 5 and extract(day from trade_date) between 15 and 21 then 'OPEX'
                    when extract(isodow from trade_date) = 5 then 'NON_OPEX'
                    else 'OTHER'
                end as bucket,
                count(*) as trade_count,
                coalesce(sum(pnl_amount), 0) as total_pnl,
                case when count(*) = 0 then 0
                     else (sum(case when outcome = 'WIN' then 1 else 0 end) * 100.0 / count(*))
                end as win_rate,
                coalesce(avg(pnl_amount), 0) as avg_pnl,
                coalesce(avg(case when pnl_amount > 0 then pnl_amount end), 0) as avg_win,
                coalesce(avg(case when pnl_amount < 0 then pnl_amount end), 0) as avg_loss
            from trades
            where user_id = :userId
              and (:accountId is null or account_id = :accountId)
              and extract(isodow from trade_date) = 5
            group by bucket
            """, nativeQuery = true)
    List<Object[]> aggregateFridayOpex(@Param("userId") Long userId, @Param("accountId") Long accountId);

}
