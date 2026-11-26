package com.tradepilot.survival;

import com.tradepilot.entity.Trade;
import com.tradepilot.importing.TradingSessionPolicy;
import com.tradepilot.survival.dto.CommitmentEvaluationResponse;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Evaluates only durable completed round trips. Broker fill reconstruction happens during import. */
@Component
public class ProtectionCommitmentEvaluator {
    private static final Set<String> SUPPORTED_EXPOSURE_ROOTS = Set.of("NQ", "MNQ", "ES", "MES");
    private static final FuturesContractSymbolNormalizer CONTRACT_SYMBOL_NORMALIZER =
            new FuturesContractSymbolNormalizer();

    public Evaluation evaluate(NextSessionProtectionCommitment commitment, List<Trade> trades, ZoneId zone) {
        List<Attempt> attempts = attempts(trades);
        List<CommitmentEvaluationResponse.RuleResult> rules = new ArrayList<>();
        boolean noAttempts = attempts.isEmpty();
        boolean invalidAttempts = attempts.size() != trades.size()
                || trades.stream().anyMatch(t -> t.getEntryTime() == null || t.getExitTime() == null
                        || !t.getExitTime().isAfter(t.getEntryTime()));
        boolean unsupportedExposureSymbol = trades.stream().anyMatch(t -> t.getSymbol() != null
                && !t.getSymbol().isBlank() && supportedExposureRoot(t) == null);
        boolean incompleteExposure = invalidAttempts || hasOverlappingAttempts(attempts)
                || attempts.stream().anyMatch(a -> a.normalizedExposure() == null);
        boolean incompletePnl = invalidAttempts || trades.stream().anyMatch(t -> t.getPnlAmount() == null);
        if (commitment.getMaxAttempts() != null) {
            rules.add(noAttempts ? notEvaluable("MAX_ATTEMPTS", String.valueOf(commitment.getMaxAttempts()),
                    "No completed attempts are available to evaluate the configured maximum.")
                    : invalidAttempts ? notEvaluable("MAX_ATTEMPTS", String.valueOf(commitment.getMaxAttempts()),
                    "An imported round trip has incomplete or invalid timestamps; attempts cannot be counted reliably.")
                    : rule("MAX_ATTEMPTS", String.valueOf(commitment.getMaxAttempts()), String.valueOf(attempts.size()),
                    attempts.size() <= commitment.getMaxAttempts(),
                    "Completed round-trip imports are counted once per flat-to-position attempt; scale-ins and partial exits are already grouped during import."));
        }
        if (commitment.getMaxNormalizedExposure() != null) {
            rules.add(noAttempts ? notEvaluable("MAX_NORMALIZED_EXPOSURE", commitment.getMaxNormalizedExposure().toPlainString(),
                    "No completed attempts are available to establish maximum exposure.")
                    : unsupportedExposureSymbol ? notEvaluable("MAX_NORMALIZED_EXPOSURE", commitment.getMaxNormalizedExposure().toPlainString(),
                    "An imported trade uses an instrument that cannot be reliably normalized; supported exposure families are NQ/MNQ and ES/MES.")
                    : incompleteExposure ? notEvaluable("MAX_NORMALIZED_EXPOSURE", commitment.getMaxNormalizedExposure().toPlainString(),
                    "An imported trade has incomplete timestamps, overlapping position intervals, invalid quantity, or missing symbol; maximum concurrent exposure cannot be established.")
                    : exposureRule(commitment.getMaxNormalizedExposure(), attempts));
        }
        if (commitment.getMaxSessionLoss() != null) {
            rules.add(sessionLossRule(commitment.getMaxSessionLoss(), trades, attempts));
        }
        if (commitment.getPostLossPauseMinutes() != null) {
            rules.add(incompletePnl
                    ? notEvaluable("POST_LOSS_PAUSE", commitment.getPostLossPauseMinutes() + " minutes",
                            "An imported trade has missing realized P&L or invalid timestamps; post-loss pauses cannot be verified.")
                    : pauseRule(commitment.getPostLossPauseMinutes(), attempts));
        }
        if (commitment.getAllowedWindowStart() != null && commitment.getAllowedWindowEnd() != null) {
            rules.add(invalidAttempts ? notEvaluable("TRADING_WINDOW",
                    formatWindow(commitment.getAllowedWindowStart(), commitment.getAllowedWindowEnd()),
                    "An imported trade has incomplete or invalid timestamps; its entry window cannot be verified.")
                    : windowRule(commitment.getAllowedWindowStart(), commitment.getAllowedWindowEnd(), attempts, zone));
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

    private boolean hasOverlappingAttempts(List<Attempt> attempts) {
        LocalDateTime latestClose = null;
        for (Attempt attempt : attempts) {
            if (latestClose != null && attempt.openedAt().isBefore(latestClose)) {
                return true;
            }
            if (latestClose == null || attempt.closedAt().isAfter(latestClose)) {
                latestClose = attempt.closedAt();
            }
        }
        return false;
    }

    private CommitmentEvaluationResponse.RuleResult sessionLossRule(BigDecimal configuredLoss, List<Trade> trades,
            List<Attempt> attempts) {
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
                .sorted(Comparator.comparing(Attempt::closedAt)
                        .thenComparing(Attempt::tradeId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal worst = BigDecimal.ZERO;
        LocalDateTime firstCrossingAt = null;
        LocalDateTime ambiguousCrossingAt = null;
        int laterTrades = 0;
        BigDecimal boundary = configuredLoss.negate();
        int index = 0;
        while (index < closed.size()) {
            LocalDateTime closedAt = closed.get(index).closedAt();
            int groupEnd = index;
            BigDecimal groupPnl = BigDecimal.ZERO;
            BigDecimal groupLosses = BigDecimal.ZERO;
            while (groupEnd < closed.size() && closed.get(groupEnd).closedAt().equals(closedAt)) {
                BigDecimal pnl = closed.get(groupEnd).pnl();
                groupPnl = groupPnl.add(pnl);
                if (pnl.signum() < 0) {
                    groupLosses = groupLosses.add(pnl);
                }
                groupEnd++;
            }

            BigDecimal worstPossibleWithinTimestamp = cumulative.add(groupLosses);
            BigDecimal cumulativeAfterTimestamp = cumulative.add(groupPnl);
            BigDecimal leastSevereMinimum = cumulative.min(cumulativeAfterTimestamp);
            if (firstCrossingAt == null
                    && worstPossibleWithinTimestamp.compareTo(boundary) <= 0
                    && leastSevereMinimum.compareTo(boundary) > 0) {
                if (ambiguousCrossingAt == null) {
                    ambiguousCrossingAt = closedAt;
                }
            }

            cumulative = cumulativeAfterTimestamp;
            if (cumulative.compareTo(worst) < 0) {
                worst = cumulative;
            }
            if (firstCrossingAt == null && cumulative.compareTo(boundary) <= 0) {
                firstCrossingAt = closedAt;
                laterTrades = closed.size() - groupEnd;
            }
            index = groupEnd;
        }
        if (firstCrossingAt == null) {
            if (ambiguousCrossingAt != null) {
                return notEvaluable("MAX_SESSION_LOSS", configuredLoss.toPlainString(),
                        "Trades sharing close timestamp " + ambiguousCrossingAt
                                + " have an ambiguous within-timestamp realized-P&L order that could change whether the loss boundary was reached.");
            }
            return rule("MAX_SESSION_LOSS", configuredLoss.toPlainString(), worst.toPlainString(), true,
                    "Worst cumulative realized P&L was " + worst.toPlainString() + "; the "
                            + configuredLoss.toPlainString() + " loss boundary was not reached.");
        }
        return rule("MAX_SESSION_LOSS", configuredLoss.toPlainString(), worst.toPlainString(), false,
                "Cumulative realized P&L first reached the " + configuredLoss.toPlainString()
                        + " loss boundary at close timestamp " + firstCrossingAt + ". "
                        + laterTrades + " completed trade(s) followed that stop condition. "
                        + "Equality counts as reaching the boundary.");
    }

    private CommitmentEvaluationResponse.RuleResult pauseRule(int configuredMinutes, List<Attempt> attempts) {
        Duration shortest = null;
        int unobservedLosses = 0;
        for (Attempt losing : attempts) {
            if (losing.pnl().signum() >= 0) {
                continue;
            }
            Attempt next = attempts.stream()
                    .filter(candidate -> candidate.openedAt().isAfter(losing.closedAt()))
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                unobservedLosses++;
                continue;
            }
            Duration pause = Duration.between(losing.closedAt(), next.openedAt());
            if (shortest == null || pause.compareTo(shortest) < 0) shortest = pause;
        }
        if (shortest != null && shortest.compareTo(Duration.ofMinutes(configuredMinutes)) < 0) {
            long minutes = shortest.toMinutes();
            return rule("POST_LOSS_PAUSE", configuredMinutes + " minutes", minutes + " minutes", false,
                    "At least one observed re-entry occurred before the configured pause elapsed; the shortest observed pause is reported.");
        }
        if (unobservedLosses > 0) {
            return notEvaluable("POST_LOSS_PAUSE", configuredMinutes + " minutes",
                    unobservedLosses + " realized losing attempt(s) had no subsequent observable re-entry; successful adherence cannot be established.");
        }
        if (shortest == null) {
            return notEvaluable("POST_LOSS_PAUSE", configuredMinutes + " minutes",
                    "No realized loss followed by a re-entry was available to test the configured pause.");
        }
        long minutes = shortest.toMinutes();
        return rule("POST_LOSS_PAUSE", configuredMinutes + " minutes", minutes + " minutes", true,
                "Every realized losing attempt had a subsequent observable re-entry at or after the configured pause; the shortest observed pause is reported.");
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

    private CommitmentEvaluationResponse.RuleResult exposureRule(BigDecimal configuredExposure, List<Attempt> attempts) {
        BigDecimal observed = attempts.stream()
                .map(Attempt::normalizedExposure)
                .max(BigDecimal::compareTo)
                .orElseThrow();
        return rule("MAX_NORMALIZED_EXPOSURE", configuredExposure.toPlainString(), observed.toPlainString(),
                observed.compareTo(configuredExposure) <= 0,
                "Maximum normalized exposure across completed attempts.");
    }

    private String supportedExposureRoot(Trade trade) {
        if (trade.getSymbol() == null || trade.getSymbol().isBlank()) {
            return null;
        }
        String symbol = trade.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (SUPPORTED_EXPOSURE_ROOTS.contains(symbol)) {
            return symbol;
        }
        var normalized = CONTRACT_SYMBOL_NORMALIZER.normalize(symbol, trade.getEntryTime());
        return normalized.valid() && SUPPORTED_EXPOSURE_ROOTS.contains(normalized.rootInstrument())
                ? normalized.rootInstrument()
                : null;
    }

    private BigDecimal normalizedExposure(Trade t) {
        if (t.getQuantity() == null || t.getQuantity() <= 0) {
            return null;
        }
        String root = supportedExposureRoot(t);
        if (root == null) {
            return null;
        }
        boolean standardContract = "NQ".equals(root) || "ES".equals(root);
        return BigDecimal.valueOf(t.getQuantity())
                .multiply(standardContract ? BigDecimal.TEN : BigDecimal.ONE);
    }
    record Attempt(Long tradeId, LocalDateTime openedAt, LocalDateTime closedAt, BigDecimal pnl, BigDecimal normalizedExposure) { }
    public record Evaluation(List<CommitmentEvaluationResponse.RuleResult> rules, ProtectionCommitmentStatus overallStatus) { }
}
