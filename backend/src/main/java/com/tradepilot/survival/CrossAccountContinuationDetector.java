package com.tradepilot.survival;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.Trade;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Detects observable, post-loss continuation across a user's accounts. The detector deliberately
 * clusters near-synchronous executions and does not infer a copier or a trader's intent.
 */
@Component
public class CrossAccountContinuationDetector {
    public static final Duration CONTINUATION_WINDOW = Duration.ofMinutes(20);
    public static final Duration SYNCHRONIZATION_TOLERANCE = Duration.ofSeconds(2);

    public List<Candidate> detect(Collection<Trade> trades, Long participatingAccountId) {
        List<Attempt> attempts = trades.stream()
                .map(this::attempt)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Attempt::entryAt).thenComparing(Attempt::tradeId))
                .toList();
        List<Cluster> losses = clusters(attempts.stream().filter(attempt -> attempt.pnl().signum() < 0).toList(), true);
        List<Cluster> entries = clusters(attempts, false);
        List<Candidate> candidates = new ArrayList<>();
        for (Cluster loss : losses) {
            for (Cluster entry : entries) {
                if (!loss.family().equals(entry.family()) || !loss.direction().equals(entry.direction())) continue;
                if (!sameUser(loss, entry)) continue;
                if (!entry.entryAt().isAfter(loss.exitAt())) continue; // No already-open or overlapping continuation.
                Duration elapsed = Duration.between(loss.exitAt(), entry.entryAt());
                if (elapsed.compareTo(CONTINUATION_WINDOW) > 0) continue;
                if (entry.exposure().compareTo(loss.exposure()) < 0) continue;
                if (!differentAccount(loss.accountIds(), entry.accountIds())) continue;
                if (participatingAccountId != null && !participates(participatingAccountId, loss, entry)) continue;
                candidates.add(candidate(loss, entry, elapsed));
                break; // A loss cluster contributes at most one earliest continuation candidate.
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparing(Candidate::laterEntryAt)
                        .thenComparing(candidate -> candidate.loss().stableId())
                        .thenComparing(candidate -> candidate.later().stableId()))
                .toList();
    }

    private List<Cluster> clusters(List<Attempt> attempts, boolean useExitTime) {
        List<Cluster> result = new ArrayList<>();
        for (Attempt attempt : attempts.stream()
                .sorted(Comparator.comparing((Attempt value) -> useExitTime ? value.exitAt() : value.entryAt()).thenComparing(Attempt::tradeId))
                .toList()) {
            LocalDateTime timestamp = useExitTime ? attempt.exitAt() : attempt.entryAt();
            Cluster current = result.isEmpty() ? null : result.get(result.size() - 1);
            if (current != null && current.family().equals(attempt.family()) && current.direction().equals(attempt.direction())
                    && current.userIds().equals(Set.of(attempt.userId()))
                    && Duration.between(current.anchorAt(), timestamp).abs().compareTo(SYNCHRONIZATION_TOLERANCE) <= 0) {
                result.set(result.size() - 1, current.add(attempt, useExitTime));
            } else {
                result.add(Cluster.of(attempt, useExitTime));
            }
        }
        return result;
    }

    private Candidate candidate(Cluster loss, Cluster later, Duration elapsed) {
        BigDecimal exposureDifference = later.exposure().subtract(loss.exposure());
        BigDecimal multiplier = loss.exposure().signum() == 0 ? null : later.exposure().divide(loss.exposure(), 6, java.math.RoundingMode.HALF_UP);
        boolean completeCoverage = loss.attempts().stream().allMatch(Attempt::completeImportCoverage)
                && later.attempts().stream().allMatch(Attempt::completeImportCoverage);
        String confidence = completeCoverage ? "STRONG" : "MODERATE";
        List<String> limitations = new ArrayList<>();
        limitations.add("Completed round trips with second-level timestamps support this observed sequence.");
        if (!completeCoverage) limitations.add("Import-batch coverage is incomplete or unavailable, so this evidence is moderate rather than conclusive.");
        if (loss.attempts().size() > 1 || later.attempts().size() > 1) {
            limitations.add("Near-synchronous account activity is grouped as one synchronized action; no copier relationship is inferred.");
        }
        return new Candidate(loss, later, elapsed.getSeconds(), exposureDifference, multiplier, confidence, List.copyOf(limitations));
    }

    private boolean participates(Long accountId, Cluster loss, Cluster later) {
        return loss.accountIds().contains(accountId) || later.accountIds().contains(accountId);
    }

    private boolean sameUser(Cluster loss, Cluster later) {
        return loss.userIds().size() == 1 && loss.userIds().equals(later.userIds());
    }

    private boolean differentAccount(Set<Long> losses, Set<Long> later) {
        return later.stream().anyMatch(accountId -> !losses.contains(accountId));
    }

    private Attempt attempt(Trade trade) {
        if (trade == null || trade.getId() == null || trade.getUser() == null || trade.getUser().getId() == null || trade.getAccount() == null || trade.getAccount().getId() == null
                || trade.getEntryTime() == null || trade.getExitTime() == null || !trade.getExitTime().isAfter(trade.getEntryTime())
                || !preciseToSeconds(trade.getEntryTime()) || !preciseToSeconds(trade.getExitTime())
                || trade.getQuantity() == null || trade.getQuantity() <= 0 || trade.getPnlAmount() == null) return null;
        String family = family(trade.getSymbol());
        String direction = direction(trade.getSide());
        if (family == null || direction == null) return null;
        Long userId = trade.getUser().getId();
        return new Attempt(trade.getId(), userId, trade.getAccount().getId(), safeAccountName(trade), trade.getSymbol(), family, direction,
                trade.getEntryTime(), trade.getExitTime(), normalizedExposure(trade), trade.getPnlAmount(), completeCoverage(trade.getImportBatch()));
    }

    private boolean preciseToSeconds(LocalDateTime timestamp) {
        return timestamp.getSecond() != 0 || timestamp.getNano() != 0;
    }

    private boolean completeCoverage(ImportBatch batch) {
        return batch != null && batch.getTotalRows() != null && batch.getImportedRows() != null
                && batch.getTotalRows().equals(batch.getImportedRows());
    }

    private String safeAccountName(Trade trade) {
        String name = trade.getAccount().getName();
        return name == null || name.isBlank() ? "Account" : name;
    }

    static String family(String symbol) {
        if (symbol == null) return null;
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MNQ") || normalized.startsWith("NQ")) return "NASDAQ";
        if (normalized.startsWith("MES") || normalized.startsWith("ES")) return "S&P";
        return null;
    }

    static String direction(String side) {
        if (side == null) return null;
        String normalized = side.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("LONG") || normalized.equals("BUY") ? "LONG"
                : normalized.equals("SHORT") || normalized.equals("SELL") ? "SHORT" : null;
    }

    static BigDecimal normalizedExposure(Trade trade) {
        String family = family(trade.getSymbol());
        if (family == null || trade.getQuantity() == null) return BigDecimal.ZERO;
        // Existing survival normalization treats a standard NQ/ES contract as ten normalized units.
        boolean standardContract = trade.getSymbol().trim().toUpperCase(Locale.ROOT).startsWith("NQ")
                || trade.getSymbol().trim().toUpperCase(Locale.ROOT).startsWith("ES");
        return BigDecimal.valueOf(trade.getQuantity()).multiply(standardContract ? BigDecimal.TEN : BigDecimal.ONE);
    }

    public record Candidate(Cluster loss, Cluster later, long elapsedSeconds, BigDecimal exposureDifference,
                            BigDecimal exposureMultiplier, String confidence, List<String> limitations) {
        public LocalDateTime laterEntryAt() { return later.entryAt(); }
    }

    public record Cluster(List<Attempt> attempts, String family, String direction, LocalDateTime anchorAt,
                          LocalDateTime entryAt, LocalDateTime exitAt, BigDecimal exposure, BigDecimal pnl,
                          Set<Long> accountIds, Set<Long> userIds, String stableId) {
        static Cluster of(Attempt attempt, boolean lossCluster) { return from(List.of(attempt), lossCluster); }
        Cluster add(Attempt attempt, boolean lossCluster) {
            List<Attempt> next = new ArrayList<>(attempts); next.add(attempt); return from(next, lossCluster);
        }
        private static Cluster from(List<Attempt> attempts, boolean lossCluster) {
            List<Attempt> ordered = attempts.stream().sorted(Comparator.comparing(Attempt::entryAt).thenComparing(Attempt::tradeId)).toList();
            LocalDateTime entry = ordered.stream().map(Attempt::entryAt).min(LocalDateTime::compareTo).orElseThrow();
            LocalDateTime exit = ordered.stream().map(Attempt::exitAt).max(LocalDateTime::compareTo).orElseThrow();
            LocalDateTime anchor = lossCluster ? ordered.stream().map(Attempt::exitAt).min(LocalDateTime::compareTo).orElseThrow() : entry;
            Set<Long> accounts = ordered.stream().map(Attempt::accountId).collect(Collectors.toCollection(LinkedHashSet::new));
            Set<Long> users = ordered.stream().map(Attempt::userId).collect(Collectors.toCollection(LinkedHashSet::new));
            String id = (lossCluster ? "loss:" : "entry:") + anchor + ":" + ordered.stream().map(Attempt::tradeId).map(String::valueOf).collect(Collectors.joining(","));
            return new Cluster(List.copyOf(ordered), ordered.get(0).family(), ordered.get(0).direction(), anchor, entry, exit,
                    ordered.stream().map(Attempt::normalizedExposure).reduce(BigDecimal.ZERO, BigDecimal::add),
                    ordered.stream().map(Attempt::pnl).reduce(BigDecimal.ZERO, BigDecimal::add), Set.copyOf(accounts), Set.copyOf(users), id);
        }
        public List<String> accountNames() { return attempts.stream().map(Attempt::accountName).distinct().toList(); }
        public List<String> symbols() { return attempts.stream().map(Attempt::symbol).distinct().toList(); }
    }

    record Attempt(Long tradeId, Long userId, Long accountId, String accountName, String symbol, String family, String direction,
                   LocalDateTime entryAt, LocalDateTime exitAt, BigDecimal normalizedExposure, BigDecimal pnl,
                   boolean completeImportCoverage) { }
}
