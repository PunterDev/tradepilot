package com.tradepilot.service;

import com.tradepilot.dto.stats.*;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.spec.TradeSpecification;
import com.tradepilot.dto.trade.TradeFilterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final TradeRepository tradeRepository;

    public DashboardSummaryResponse getDashboardSummary(User user, LocalDate startDate, LocalDate endDate) {
        return buildSummary(getTrades(user, startDate, endDate));
    }

    public List<MetricItemResponse> getStatsBySetup(User user, LocalDate s, LocalDate e) {
        return group(getTrades(user, s, e), t -> t.getSetup().name());
    }

    public List<MetricItemResponse> getStatsBySession(User user, LocalDate s, LocalDate e) {
        return group(getTrades(user, s, e), t -> t.getSession().name());
    }

    public List<MetricItemResponse> getStatsBySymbol(User user, LocalDate s, LocalDate e) {
        return group(getTrades(user, s, e), Trade::getSymbol);
    }

    public List<MetricItemResponse> getStatsByWeekday(User user, LocalDate s, LocalDate e) {
        List<Trade> trades = getTrades(user, s, e);
        Map<String, List<Trade>> grouped = trades.stream().collect(Collectors.groupingBy(t -> formatWeekday(t.getTradeDate().getDayOfWeek())));
        return List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").stream().filter(grouped::containsKey).map(d -> buildMetricItem(d, grouped.get(d))).toList();
    }

    private List<Trade> getTrades(User user, LocalDate s, LocalDate e) {
        if (s != null && e != null && s.isAfter(e))
            throw new IllegalArgumentException("startDate cannot be after endDate");
        TradeFilterRequest filter = TradeFilterRequest.builder().startDate(s).endDate(e).build();
        return tradeRepository.findAll(TradeSpecification.withFilters(user, filter));
    }

    private DashboardSummaryResponse buildSummary(List<Trade> trades) {
        MetricItemResponse m = buildMetricItem("summary", trades);
        return DashboardSummaryResponse.builder().totalTrades(m.getTotalTrades()).wins(m.getWins()).losses(m.getLosses()).breakevens(m.getBreakevens()).totalPnl(m.getTotalPnl()).averageR(m.getAverageR()).winRate(m.getWinRate()).build();
    }

    private List<MetricItemResponse> group(List<Trade> trades, Function<Trade, String> classifier) {
        return trades.stream().collect(Collectors.groupingBy(classifier)).entrySet().stream().map(e -> buildMetricItem(e.getKey(), e.getValue())).sorted(Comparator.comparing(MetricItemResponse::getTotalPnl).reversed()).toList();
    }

    private MetricItemResponse buildMetricItem(String label, List<Trade> trades) {
        long total = trades.size();
        long wins = trades.stream().filter(t -> t.getOutcome() == OutcomeType.WIN).count();
        long losses = trades.stream().filter(t -> t.getOutcome() == OutcomeType.LOSS).count();
        long be = trades.stream().filter(t -> t.getOutcome() == OutcomeType.BE).count();
        BigDecimal totalPnl = trades.stream().map(t -> z(t.getPnlAmount())).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalR = trades.stream().map(t -> z(t.getPnlR())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgR = total == 0 ? BigDecimal.ZERO : totalR.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        BigDecimal winRate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf((wins * 100.0) / total).setScale(2, RoundingMode.HALF_UP);
        return MetricItemResponse.builder().label(label).totalTrades(total).wins(wins).losses(losses).breakevens(be).totalPnl(totalPnl).averageR(avgR).winRate(winRate).build();
    }

    private BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String formatWeekday(DayOfWeek d) {
        return d.toString().charAt(0) + d.toString().substring(1).toLowerCase();
    }
}
