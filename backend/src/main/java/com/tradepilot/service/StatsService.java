package com.tradepilot.service;

import com.tradepilot.dto.stats.*;
import com.tradepilot.entity.User;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final TradeRepository tradeRepository;
    private final TradingAccountService tradingAccountService;
    private final ObservabilityService observabilityService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(User user, Long accountId, LocalDate startDate, LocalDate endDate) {
        validateFilters(user, accountId, startDate, endDate);
        return mapSummary((Object[]) observabilityService.timeQuery(
                "dashboard_summary",
                () -> tradeRepository.aggregateSummary(user.getId(), accountId, startDate, endDate)
        ));
    }

    @Transactional(readOnly = true)
    public List<MetricItemResponse> getStatsBySetup(User user, Long accountId, LocalDate s, LocalDate e) {
        return getStatsByDimension(user, accountId, s, e, "setup");
    }

    @Transactional(readOnly = true)
    public List<MetricItemResponse> getStatsBySession(User user, Long accountId, LocalDate s, LocalDate e) {
        return getStatsByDimension(user, accountId, s, e, "session");
    }

    @Transactional(readOnly = true)
    public List<MetricItemResponse> getStatsBySymbol(User user, Long accountId, LocalDate s, LocalDate e) {
        return getStatsByDimension(user, accountId, s, e, "symbol");
    }

    @Transactional(readOnly = true)
    public List<MetricItemResponse> getStatsByWeekday(User user, Long accountId, LocalDate s, LocalDate e) {
        return getStatsByDimension(user, accountId, s, e, "weekday").stream()
                .map(item -> {
                    item.setLabel(formatWeekdayLabel(item.getLabel()));
                    return item;
                })
                .toList();
    }

    private List<MetricItemResponse> getStatsByDimension(User user, Long accountId, LocalDate s, LocalDate e, String dimension) {
        validateFilters(user, accountId, s, e);
        List<Object[]> rows = switch (dimension) {
            case "setup" -> observabilityService.timeQuery("stats_by_setup", () -> tradeRepository.aggregateBySetup(user.getId(), accountId, s, e));
            case "session" -> observabilityService.timeQuery("stats_by_session", () -> tradeRepository.aggregateBySession(user.getId(), accountId, s, e));
            case "symbol" -> observabilityService.timeQuery("stats_by_symbol", () -> tradeRepository.aggregateBySymbol(user.getId(), accountId, s, e));
            case "weekday" -> observabilityService.timeQuery("stats_by_weekday", () -> tradeRepository.aggregateByWeekday(user.getId(), accountId, s, e));
            default -> List.<Object[]>of();
        };
        return rows.stream().map(this::mapMetric).toList();
    }

    private void validateFilters(User user, Long accountId, LocalDate s, LocalDate e) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        if (accountId != null) {
            tradingAccountService.getAccountForImport(user, accountId);
        }
        if (s != null && e != null && s.isAfter(e))
            throw new IllegalArgumentException("startDate cannot be after endDate");
    }

    private DashboardSummaryResponse mapSummary(Object[] row) {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTotalTrades(longValue(row, 0));
        response.setWins(longValue(row, 1));
        response.setLosses(longValue(row, 2));
        response.setBreakevens(longValue(row, 3));
        response.setTotalPnl(decimalValue(row, 4));
        response.setAverageR(decimalValue(row, 5));
        response.setWinRate(decimalValue(row, 6));
        return response;
    }

    private MetricItemResponse mapMetric(Object[] row) {
        MetricItemResponse response = new MetricItemResponse();
        response.setLabel(String.valueOf(row[0]));
        response.setTotalTrades(longValue(row, 1));
        response.setWins(longValue(row, 2));
        response.setLosses(longValue(row, 3));
        response.setBreakevens(longValue(row, 4));
        response.setTotalPnl(decimalValue(row, 5));
        response.setAverageR(decimalValue(row, 6));
        response.setWinRate(decimalValue(row, 7));
        return response;
    }

    private long longValue(Object[] row, int index) {
        Object value = row[index];
        return value instanceof Number number ? number.longValue() : 0;
    }

    private BigDecimal decimalValue(Object[] row, int index) {
        Object value = row[index];
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private String formatWeekdayLabel(String value) {
        return switch (value) {
            case "1", "1.0" -> "Monday";
            case "2", "2.0" -> "Tuesday";
            case "3", "3.0" -> "Wednesday";
            case "4", "4.0" -> "Thursday";
            case "5", "5.0" -> "Friday";
            case "6", "6.0" -> "Saturday";
            case "7", "7.0" -> "Sunday";
            default -> "Unspecified";
        };
    }
}
