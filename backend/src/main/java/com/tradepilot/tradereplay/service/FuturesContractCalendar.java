package com.tradepilot.tradereplay.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FuturesContractCalendar {
    private static final Set<String> SUPPORTED_ROOTS = Set.of("ES", "MES", "NQ", "MNQ");
    private static final List<Integer> QUARTER_MONTHS = List.of(3, 6, 9, 12);
    private static final Map<Integer, String> MONTH_CODE_BY_MONTH = Map.of(3, "H", 6, "M", 9, "U", 12, "Z");
    private static final Pattern NORMALIZED_CONTRACT = Pattern.compile("^(MES|MNQ|ES|NQ)([HMUZ])(\\d)$");

    private final FuturesContractSymbolNormalizer normalizer;

    public FuturesContractCalendar(FuturesContractSymbolNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public Optional<ParsedContract> parse(String contractSymbol, LocalDateTime executionTime) {
        ContractSymbolNormalizationResult normalized = normalizer.normalize(contractSymbol, executionTime);
        if (!normalized.valid()) {
            return Optional.empty();
        }
        Matcher matcher = NORMALIZED_CONTRACT.matcher(normalized.normalizedContractSymbol());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String root = matcher.group(1);
        String monthCode = matcher.group(2);
        int month = monthForCode(monthCode);
        int year = resolveOneDigitYear(Integer.parseInt(matcher.group(3)), executionTime);
        return Optional.of(new ParsedContract(root, monthCode, month, year, normalized.normalizedContractSymbol()));
    }

    public Optional<String> expectedActiveContract(String rootSymbol, LocalDate tradeDate) {
        String root = normalizer.rootInstrument(rootSymbol);
        if (root == null || !SUPPORTED_ROOTS.contains(root) || tradeDate == null) {
            return Optional.empty();
        }
        Quarter quarter = expectedQuarter(tradeDate);
        return Optional.of(root + MONTH_CODE_BY_MONTH.get(quarter.month()) + (quarter.year() % 10));
    }

    public boolean contractValidForDate(String contractSymbol, LocalDate tradeDate, LocalDateTime executionTime) {
        if (tradeDate == null) {
            return false;
        }
        return parse(contractSymbol, executionTime)
                .map(contract -> !tradeDate.isAfter(expirationDate(contract.month(), contract.year())))
                .orElse(false);
    }

    public LocalDate expirationDate(String contractSymbol, LocalDateTime executionTime) {
        return parse(contractSymbol, executionTime)
                .map(contract -> expirationDate(contract.month(), contract.year()))
                .orElse(null);
    }

    public LocalDate expirationDate(int contractMonth, int contractYear) {
        LocalDate first = LocalDate.of(contractYear, contractMonth, 1);
        int daysUntilFriday = Math.floorMod(DayOfWeek.FRIDAY.getValue() - first.getDayOfWeek().getValue(), 7);
        return first.plusDays(daysUntilFriday + 14L);
    }

    private Quarter expectedQuarter(LocalDate tradeDate) {
        for (int month : QUARTER_MONTHS) {
            LocalDate expiration = expirationDate(month, tradeDate.getYear());
            if (!tradeDate.isAfter(expiration)) {
                return new Quarter(month, tradeDate.getYear());
            }
        }
        return new Quarter(3, tradeDate.getYear() + 1);
    }

    private int monthForCode(String monthCode) {
        return switch (monthCode) {
            case "H" -> 3;
            case "M" -> 6;
            case "U" -> 9;
            case "Z" -> 12;
            default -> throw new IllegalArgumentException("Unsupported futures month code.");
        };
    }

    private int resolveOneDigitYear(int yearDigit, LocalDateTime executionTime) {
        int referenceYear = executionTime == null ? LocalDate.now().getYear() : executionTime.getYear();
        int decade = (referenceYear / 10) * 10;
        int best = decade + yearDigit;
        int bestDistance = Math.abs(best - referenceYear);
        for (int candidate : List.of(decade - 10 + yearDigit, decade + 10 + yearDigit)) {
            int distance = Math.abs(candidate - referenceYear);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private record Quarter(int month, int year) {}

    public record ParsedContract(String root, String monthCode, int month, int year, String normalizedSymbol) {}
}
