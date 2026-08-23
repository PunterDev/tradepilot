package com.tradepilot.tradereplay.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FuturesContractSymbolNormalizer {
    private static final Set<String> SUPPORTED_ROOTS = Set.of("MES", "MNQ", "ES", "NQ");
    private static final Map<String, Integer> MONTH_BY_CODE = Map.of("H", 3, "M", 6, "U", 9, "Z", 12);
    private static final Map<Integer, String> CODE_BY_MONTH = Map.of(3, "H", 6, "M", 9, "U", 12, "Z");
    private static final Pattern CODE_PATTERN = Pattern.compile("^(MES|MNQ|ES|NQ)([HMUZ])(\\d{1,4})$");
    private static final Pattern NUMERIC_MONTH_PATTERN = Pattern.compile("^(MES|MNQ|ES|NQ)(0?[369]|03|06|09|12)(\\d{2}|\\d{4})$");

    public ContractSymbolNormalizationResult normalize(String symbol, LocalDateTime executionTime) {
        String original = symbol;
        String cleaned = clean(symbol);
        if (cleaned == null) {
            return ContractSymbolNormalizationResult.failed(original, "CONTRACT_SYMBOL_MISSING", "Trade symbol is missing.");
        }
        if (SUPPORTED_ROOTS.contains(cleaned)) {
            return ContractSymbolNormalizationResult.failed(original, "CONTRACT_REQUIRED", "Replay requires the exact futures contract.");
        }
        Matcher code = CODE_PATTERN.matcher(cleaned);
        if (code.matches()) {
            return normalizeParts(original, code.group(1), code.group(2), code.group(3), executionTime);
        }
        Matcher numeric = NUMERIC_MONTH_PATTERN.matcher(cleaned);
        if (numeric.matches()) {
            int month = Integer.parseInt(numeric.group(2));
            String monthCode = CODE_BY_MONTH.get(month);
            if (monthCode == null) {
                return ContractSymbolNormalizationResult.failed(original, "INVALID_CONTRACT_MONTH", "Only quarterly equity-index futures contracts are supported.");
            }
            return normalizeParts(original, numeric.group(1), monthCode, numeric.group(3), executionTime);
        }
        String root = rootFrom(cleaned);
        if (root != null) {
            return ContractSymbolNormalizationResult.failed(original, "INVALID_CONTRACT_FORMAT", "Contract symbol format is not supported.");
        }
        return ContractSymbolNormalizationResult.failed(original, "UNSUPPORTED_INSTRUMENT", "AI Trade Replay currently supports MES, MNQ, ES, and NQ futures contracts.");
    }

    public String rootInstrument(String symbol) {
        String cleaned = clean(symbol);
        return cleaned == null ? null : rootFrom(cleaned);
    }

    public List<ContractCandidate> candidates(String root, LocalDateTime executionTime) {
        String normalizedRoot = rootInstrument(root);
        if (normalizedRoot == null || !SUPPORTED_ROOTS.contains(normalizedRoot) || executionTime == null) {
            return List.of();
        }
        List<Quarter> quarters = quartersAround(executionTime);
        int likelyIndex = mostLikelyIndex(quarters, executionTime);
        List<ContractCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < quarters.size(); i++) {
            Quarter quarter = quarters.get(i);
            String code = CODE_BY_MONTH.get(quarter.month());
            String normalizedSymbol = normalizedRoot + code + (quarter.year() % 10);
            String monthName = Month.of(quarter.month()).getDisplayName(TextStyle.FULL, Locale.US);
            candidates.add(new ContractCandidate(monthName + " " + quarter.year() + " - " + normalizedSymbol, normalizedRoot, normalizedSymbol, code, quarter.month(), quarter.year(), i == likelyIndex));
        }
        return candidates;
    }

    private ContractSymbolNormalizationResult normalizeParts(String original, String root, String monthCode, String yearText, LocalDateTime executionTime) {
        if (!SUPPORTED_ROOTS.contains(root) || !MONTH_BY_CODE.containsKey(monthCode)) {
            return ContractSymbolNormalizationResult.failed(original, "UNSUPPORTED_INSTRUMENT", "Unsupported futures product or month.");
        }
        Integer year = resolveYear(yearText, executionTime);
        if (year == null) {
            return ContractSymbolNormalizationResult.failed(original, "INVALID_CONTRACT_YEAR", "Contract year is ambiguous for this trade timestamp.");
        }
        return ContractSymbolNormalizationResult.valid(root, original, root + monthCode + (year % 10));
    }

    private Integer resolveYear(String yearText, LocalDateTime executionTime) {
        try {
            int parsed = Integer.parseInt(yearText);
            if (yearText.length() == 4) {
                return parsed >= 2000 && parsed <= 2099 ? parsed : null;
            }
            if (yearText.length() == 2) {
                int year = 2000 + parsed;
                return plausible(year, executionTime) ? year : null;
            }
            if (yearText.length() == 1) {
                if (executionTime == null) return null;
                int decade = (executionTime.getYear() / 10) * 10;
                List<Integer> possible = List.of(decade + parsed, decade - 10 + parsed, decade + 10 + parsed);
                return possible.stream().filter(year -> plausible(year, executionTime)).findFirst().orElse(null);
            }
            return null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean plausible(int year, LocalDateTime executionTime) {
        return year >= 2000 && year <= 2099 && (executionTime == null || Math.abs(year - executionTime.getYear()) <= 1);
    }

    private List<Quarter> quartersAround(LocalDateTime executionTime) {
        int year = executionTime.getYear();
        List<Quarter> all = new ArrayList<>();
        for (int y = year - 1; y <= year + 1; y++) {
            all.add(new Quarter(3, y));
            all.add(new Quarter(6, y));
            all.add(new Quarter(9, y));
            all.add(new Quarter(12, y));
        }
        int center = mostLikelyIndex(all, executionTime);
        int from = Math.max(0, center - 1);
        int to = Math.min(all.size(), center + 2);
        return all.subList(from, to);
    }

    private int mostLikelyIndex(List<Quarter> quarters, LocalDateTime executionTime) {
        int month = executionTime.getMonthValue();
        int year = executionTime.getYear();
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < quarters.size(); i++) {
            Quarter q = quarters.get(i);
            int distance = Math.abs((q.year() * 12 + q.month()) - (year * 12 + month));
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private String rootFrom(String cleaned) {
        if (cleaned.startsWith("MES")) return "MES";
        if (cleaned.startsWith("MNQ")) return "MNQ";
        if (cleaned.startsWith("ES")) return "ES";
        if (cleaned.startsWith("NQ")) return "NQ";
        return null;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("/")) upper = upper.substring(1);
        return upper.replaceAll("\\s+", "").replace("-", "").replace("_", "").replace(".", "");
    }

    private record Quarter(int month, int year) {}
}
