package com.tradepilot.playbook.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.playbook.dto.PlaybookMetricsResponse;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PlaybookMetricsService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public PlaybookMetricsResponse calculate(List<TradePlaybookAssignment> assignments) {
        List<TradePlaybookAssignment> ordered = assignments.stream()
                .sorted(Comparator.comparing((TradePlaybookAssignment a) -> a.getTrade().getTradeDate()).thenComparing(a -> a.getTrade().getId()))
                .toList();
        int total = ordered.size();
        if (total == 0) {
            return new PlaybookMetricsResponse(0, ZERO, ZERO, ZERO, ZERO, ZERO, null, null, null, null, null, null, null, ZERO, 0, 0, ZERO, 0, 0, 0, null, null, null, null, ZERO, ZERO, null);
        }

        BigDecimal netPnl = sum(ordered, false);
        BigDecimal totalR = sum(ordered, true);
        long wins = ordered.stream().filter(a -> compare(pnl(a), BigDecimal.ZERO) > 0).count();
        long losses = ordered.stream().filter(a -> compare(pnl(a), BigDecimal.ZERO) < 0).count();
        long scratches = total - wins - losses;
        BigDecimal grossWinR = ordered.stream().map(this::r).filter(v -> compare(v, BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossLossR = ordered.stream().map(this::r).filter(v -> compare(v, BigDecimal.ZERO) < 0).reduce(BigDecimal.ZERO, BigDecimal::add).abs();
        BigDecimal profitFactor = grossLossR.compareTo(BigDecimal.ZERO) == 0 ? null : grossWinR.divide(grossLossR, 2, RoundingMode.HALF_UP);
        BigDecimal averageR = avg(ordered.stream().map(this::r).toList());
        BigDecimal expectancy = averageR;
        BigDecimal avgWin = avg(ordered.stream().map(this::r).filter(v -> compare(v, BigDecimal.ZERO) > 0).toList());
        BigDecimal avgLoss = avg(ordered.stream().map(this::r).filter(v -> compare(v, BigDecimal.ZERO) < 0).toList());
        BigDecimal largestWin = ordered.stream().map(this::r).max(BigDecimal::compareTo).orElse(ZERO);
        BigDecimal largestLoss = ordered.stream().map(this::r).min(BigDecimal::compareTo).orElse(ZERO);
        BigDecimal drawdown = maxDrawdown(ordered);
        Set<LocalDate> days = new HashSet<>();
        Set<LocalDate> winningDays = new HashSet<>();
        for (TradePlaybookAssignment assignment : ordered) {
            days.add(assignment.getTrade().getTradeDate());
            if (compare(pnl(assignment), BigDecimal.ZERO) > 0) winningDays.add(assignment.getTrade().getTradeDate());
        }
        List<TradePlaybookAssignment> compliant = ordered.stream().filter(a -> a.getComplianceScore() != null && a.getComplianceScore() >= 70).toList();
        List<TradePlaybookAssignment> noncompliant = ordered.stream().filter(a -> a.getComplianceScore() != null && a.getComplianceScore() < 70).toList();
        BigDecimal compliantAvg = avg(compliant.stream().map(this::r).toList());
        BigDecimal noncompliantAvg = avg(noncompliant.stream().map(this::r).toList());
        BigDecimal compliantWinRate = rate(compliant.stream().filter(a -> compare(pnl(a), BigDecimal.ZERO) > 0).count(), compliant.size());
        BigDecimal noncompliantWinRate = rate(noncompliant.stream().filter(a -> compare(pnl(a), BigDecimal.ZERO) > 0).count(), noncompliant.size());
        BigDecimal dataCompleteness = rate(ordered.stream().filter(a -> a.getReviewedByUser() != null && a.getReviewedByUser()).count(), total);
        LocalDate lastUsed = ordered.stream().map(a -> a.getTrade().getTradeDate()).max(LocalDate::compareTo).orElse(null);

        return new PlaybookMetricsResponse(total, scale(totalR), scale(netPnl), rate(wins, total), rate(losses, total), rate(scratches, total), profitFactor, averageR, expectancy, avgWin, avgLoss, largestWin, largestLoss, drawdown, streak(ordered, true), streak(ordered, false), rate(winningDays.size(), days.size()), days.size(), compliant.size(), noncompliant.size(), compliantAvg, noncompliantAvg, compliantWinRate, noncompliantWinRate, diff(compliantAvg, noncompliantAvg), dataCompleteness, lastUsed);
    }

    private BigDecimal sum(List<TradePlaybookAssignment> assignments, boolean r) {
        return assignments.stream().map(r ? this::r : this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal pnl(TradePlaybookAssignment assignment) {
        return value(assignment.getTrade().getPnlAmount());
    }

    private BigDecimal r(TradePlaybookAssignment assignment) {
        Trade trade = assignment.getTrade();
        if (trade.getPnlR() != null) return value(trade.getPnlR());
        BigDecimal risk = trade.getRiskAmount();
        if (risk == null && trade.getAccount() != null) risk = trade.getAccount().getDefaultRiskAmount();
        if (risk == null || risk.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return value(trade.getPnlAmount()).divide(risk, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal maxDrawdown(List<TradePlaybookAssignment> assignments) {
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        for (TradePlaybookAssignment assignment : assignments) {
            equity = equity.add(r(assignment));
            if (equity.compareTo(peak) > 0) peak = equity;
            BigDecimal dd = peak.subtract(equity);
            if (dd.compareTo(max) > 0) max = dd;
        }
        return scale(max.negate());
    }

    private int streak(List<TradePlaybookAssignment> assignments, boolean wins) {
        int best = 0;
        int current = 0;
        for (TradePlaybookAssignment assignment : assignments) {
            int cmp = compare(pnl(assignment), BigDecimal.ZERO);
            boolean hit = wins ? cmp > 0 : cmp < 0;
            current = hit ? current + 1 : 0;
            best = Math.max(best, current);
        }
        return best;
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        return scale(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) return null;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal diff(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return scale(a.subtract(b));
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private int compare(BigDecimal a, BigDecimal b) {
        return value(a).compareTo(value(b));
    }
}
