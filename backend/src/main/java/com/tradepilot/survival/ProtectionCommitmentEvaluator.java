package com.tradepilot.survival;

import com.tradepilot.entity.Trade;
import com.tradepilot.importing.TradingSessionPolicy;
import com.tradepilot.survival.dto.CommitmentEvaluationResponse;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Evaluates only durable completed round trips. Broker fill reconstruction happens during import. */
@Component
public class ProtectionCommitmentEvaluator {
    public Evaluation evaluate(NextSessionProtectionCommitment commitment, List<Trade> trades, ZoneId zone) {
        List<Attempt> attempts = attempts(trades);
        List<CommitmentEvaluationResponse.RuleResult> rules = new ArrayList<>();
        if (commitment.getMaxAttempts() != null) {
            rules.add(rule("MAX_ATTEMPTS", String.valueOf(commitment.getMaxAttempts()), String.valueOf(attempts.size()),
                    attempts.size() <= commitment.getMaxAttempts(),
                    "Completed round-trip imports are counted once per flat-to-position attempt; scale-ins and partial exits are already grouped during import."));
        }
        if (commitment.getMaxNormalizedExposure() != null) {
            BigDecimal observed = attempts.stream().map(Attempt::normalizedExposure).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            rules.add(rule("MAX_NORMALIZED_EXPOSURE", commitment.getMaxNormalizedExposure().toPlainString(), observed.toPlainString(),
                    observed.compareTo(commitment.getMaxNormalizedExposure()) <= 0,
                    "Maximum normalized exposure across completed attempts."));
        }
        if (commitment.getMaxSessionLoss() != null) {
            rules.add(sessionLossRule(commitment.getMaxSessionLoss(), trades, attempts));
        }
        if (commitment.getPostLossPauseMinutes() != null) rules.add(pauseRule(commitment.getPostLossPauseMinutes(), attempts));
        if (commitment.getAllowedWindowStart() != null && commitment.getAllowedWindowEnd() != null) {
            rules.add(windowRule(commitment.getAllowedWindowStart(), commitment.getAllowedWindowEnd(), attempts, zone));
        }
        if (Boolean.TRUE.equals(commitment.getRequireFreshSetupConfirmation())) {
            rules.add(notEvaluable("FRESH_SETUP_CONFIRMATION", "Enabled", "Completed-trade imports do not provide contemporaneous confirmation evidence."));
        }
        return new Evaluation(rules, overallStatus(rules));
    }

    /** Every persisted Trade is a completed round trip: parsers only persist after the broker position returns flat. */
    List<Attempt> attempts(List<Trade> trades) {
        return trades.stream().filter(t -> t.getEntryTime() != null && t.getExitTime() != null)
                .sorted(Comparator.comparing(Trade::getEntryTime).thenComparing(Trade::getId, Comparator.nullsLast(Long::compareTo)))
                .map(t -> new Attempt(t.getId(), t.getEntryTime(), t.getExitTime(), pnl(t), normalizedExposure(t)))
                .toList();
    }

    private CommitmentEvaluationResponse.RuleResult sessionLossRule(BigDecimal configuredLoss, List<Trade> trades,
            List<Attempt> attempts) {
        // A zero-filled null P&L or an invalid completed-trade interval can falsely make
        // an exceeded loss boundary appear FOLLOWED. Decline to evaluate until import data
        // establishes an auditable realized P&L sequence for every round trip.
        if (trades.isEmpty()) {
            return notEvaluable("MAX_SESSION_LOSS", configuredLoss.toPlainString(),
                    "No completed trades are available to establish realized session P&L.");
        }
        if (attempts.size() != trades.size() || trades.stream().anyMatch(t ->
                t.getPnlAmount() == null
                        || t.getEntryTime() == null || t.getExitTime() == null
                        || !t.getExitTime().isAfter(t.getEntryTime()))) {
            return notEvaluable("MAX_SESSION_LOSS", configuredLoss.toPlainString(),
                    "An imported trade has missing realized P&L, incomplete timestamps, or an invalid entry/exit sequence.");
        }

        List<Attempt> closed = attempts.stream()
                .sorted(Comparator.comparing(Attempt::closedAt).thenComparing(Attempt::tradeId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (int index = 1; index < closed.size(); index++) {
            Attempt prior = closed.get(index - 1);
            Attempt current = closed.get(index);
            if (current.openedAt().isBefore(prior.closedAt()) || current.closedAt().equals(prior.closedAt())) {
                return notEvaluable("MAX_SESSION_LOSS", configuredLoss.toPlainString(),
                        "Completed trade timestamps overlap or share a close timestamp, so a reliable realized-P&L sequence cannot be established.");
            }
        }

        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal worst = BigDecimal.ZERO;
        Attempt firstCrossing = null;
        int laterTrades = 0;
        BigDecimal boundary = configuredLoss.negate();
        for (Attempt attempt : closed) {
            cumulative = cumulative.add(attempt.pnl());
            if (cumulative.compareTo(worst) < 0) worst = cumulative;
            // Equality means the trader reached the configured stop boundary and is therefore a breach.
            if (firstCrossing == null && cumulative.compareTo(boundary) <= 0) {
                firstCrossing = attempt;
            } else if (firstCrossing != null) {
                laterTrades++;
            }
        }
        if (firstCrossing == null) {
            return rule("MAX_SESSION_LOSS", configuredLoss.toPlainString(), worst.toPlainString(), true,
                    "Worst cumulative realized P&L was " + worst.toPlainString() + "; the " + configuredLoss.toPlainString()
                            + " loss boundary was not reached.");
        }
        return rule("MAX_SESSION_LOSS", configuredLoss.toPlainString(), worst.toPlainString(), false,
                "Cumulative realized P&L first reached the " + configuredLoss.toPlainString() + " loss boundary after trade "
                        + firstCrossing.tradeId() + " closed at " + firstCrossing.closedAt() + ". "
                        + laterTrades + " completed trade(s) followed that stop condition. Equality counts as reaching the boundary.");
    }

    private CommitmentEvaluationResponse.RuleResult pauseRule(int configuredMinutes, List<Attempt> attempts) {
        Duration shortest = null;
        for (int i = 0; i + 1 < attempts.size(); i++) {
            Attempt losing = attempts.get(i);
            Attempt next = attempts.get(i + 1);
            if (losing.pnl().signum() >= 0) continue;
            Duration pause = Duration.between(losing.closedAt(), next.openedAt());
            if (!next.openedAt().isAfter(losing.closedAt())) {
                return notEvaluable("POST_LOSS_PAUSE", configuredMinutes + " minutes",
                        "A later attempt overlaps or shares the loss close timestamp, so the post-loss sequence cannot be established.");
            }
            if (shortest == null || pause.compareTo(shortest) < 0) shortest = pause;
        }
        if (shortest == null) return notEvaluable("POST_LOSS_PAUSE", configuredMinutes + " minutes", "No post-loss re-entry occurred; the configured pause was not tested.");
        long minutes = shortest.toMinutes();
        return rule("POST_LOSS_PAUSE", configuredMinutes + " minutes", minutes + " minutes", minutes >= configuredMinutes,
                "Every realized losing attempt is checked against its next completed-attempt entry; the shortest observed pause is reported.");
    }

    private CommitmentEvaluationResponse.RuleResult windowRule(LocalTime start, LocalTime end, List<Attempt> attempts, ZoneId zone) {
        if (attempts.isEmpty()) return notEvaluable("TRADING_WINDOW", formatWindow(start, end), "No completed attempts were available.");
        for (Attempt attempt : attempts) {
            // Imports persist exchange-local business timestamps. Do not reinterpret LocalDateTime through an offset,
            // which would silently move entries through a DST gap.
            LocalTime local = attempt.openedAt().toLocalTime();
            if (!contains(start, end, local)) {
                return rule("TRADING_WINDOW", formatWindow(start, end), local.toString(), false,
                        "Entry at " + local + " was outside the configured window. Start is inclusive and end is exclusive.");
            }
        }
        return rule("TRADING_WINDOW", formatWindow(start, end), "All " + attempts.size() + " entries in window", true,
                "Every completed-attempt entry was within the " + zone.getId() + " configured window. Start is inclusive and end is exclusive.");
    }

    private boolean contains(LocalTime start, LocalTime end, LocalTime value) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) return !value.isBefore(start) && value.isBefore(end);
        return !value.isBefore(start) || value.isBefore(end);
    }
    private String formatWindow(LocalTime start, LocalTime end) { return start + "–" + end; }
    private CommitmentEvaluationResponse.RuleResult rule(String name, String configured, String observed, boolean followed, String evidence) {
        return new CommitmentEvaluationResponse.RuleResult(name, followed ? "FOLLOWED" : "BROKEN", configured, observed, evidence);
    }
    private CommitmentEvaluationResponse.RuleResult notEvaluable(String name, String configured, String evidence) {
        return new CommitmentEvaluationResponse.RuleResult(name, "NOT_EVALUABLE", configured, "—", evidence);
    }
    private ProtectionCommitmentStatus overallStatus(List<CommitmentEvaluationResponse.RuleResult> rules) {
        if (rules.isEmpty() || rules.stream().allMatch(r -> "NOT_EVALUABLE".equals(r.status()))) return ProtectionCommitmentStatus.NOT_EVALUABLE;
        if (rules.stream().anyMatch(r -> "BROKEN".equals(r.status()))) return ProtectionCommitmentStatus.BROKEN;
        if (rules.stream().anyMatch(r -> "NOT_EVALUABLE".equals(r.status()))) return ProtectionCommitmentStatus.PARTIALLY_OBSERVED;
        return ProtectionCommitmentStatus.COMPLETED;
    }
    private BigDecimal pnl(Trade t) { return t.getPnlAmount() == null ? BigDecimal.ZERO : t.getPnlAmount(); }
    private BigDecimal normalizedExposure(Trade t) {
        int quantity = t.getQuantity() == null ? 0 : t.getQuantity();
        String symbol = t.getSymbol() == null ? "" : t.getSymbol().toUpperCase(Locale.ROOT);
        return BigDecimal.valueOf(quantity * (symbol.startsWith("NQ") || symbol.startsWith("ES") ? 10L : 1L));
    }
    record Attempt(Long tradeId, LocalDateTime openedAt, LocalDateTime closedAt, BigDecimal pnl, BigDecimal normalizedExposure) { }
    public record Evaluation(List<CommitmentEvaluationResponse.RuleResult> rules, ProtectionCommitmentStatus overallStatus) { }
}
