package com.tradepilot.ai.memory.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TradeMemoryNormalizer {
    public String normalizeMistake(String value) {
        String text = normalizeText(value);
        if (text == null) return null;
        if (containsAny(text, "early exit", "exited too early", "cut winner", "closed too soon", "didn't hold", "did not hold")) return "CUT_WINNER_EARLY";
        if (containsAny(text, "revenge")) return "REVENGE_TRADE";
        if (containsAny(text, "fomo", "chased")) return "FOMO_ENTRY";
        if (containsAny(text, "overtrad")) return "OVERTRADING";
        if (containsAny(text, "late entry")) return "LATE_ENTRY";
        if (containsAny(text, "early entry")) return "EARLY_ENTRY";
        if (containsAny(text, "moved stop", "move stop")) return "MOVED_STOP";
        if (containsAny(text, "ignored bias", "against bias")) return "IGNORED_BIAS";
        if (containsAny(text, "daily loss")) return "BROKE_DAILY_LOSS_LIMIT";
        if (containsAny(text, "oversized", "too much size", "over size")) return "OVERSIZED_POSITION";
        if (containsAny(text, "outside session")) return "TRADED_OUTSIDE_SESSION";
        if (containsAny(text, "no setup", "unconfirmed", "no confirmed")) return "NO_CONFIRMED_SETUP";
        return enumish(text, "OTHER");
    }

    public String normalizeEmotion(String value) {
        String text = normalizeText(value);
        if (text == null) return null;
        if (containsAny(text, "fear", "scared")) return "FEAR";
        if (containsAny(text, "fomo")) return "FOMO";
        if (containsAny(text, "frustrat")) return "FRUSTRATION";
        if (containsAny(text, "anger", "angry")) return "ANGER";
        if (containsAny(text, "greed")) return "GREED";
        if (containsAny(text, "impatient")) return "IMPATIENCE";
        if (containsAny(text, "overconfident")) return "OVERCONFIDENCE";
        if (containsAny(text, "hesitat")) return "HESITATION";
        if (containsAny(text, "calm")) return "CALM";
        if (containsAny(text, "neutral")) return "NEUTRAL";
        return enumish(text, "OTHER");
    }

    public String normalizeSetup(String value) {
        String text = normalizeText(value);
        return text == null ? null : enumish(text, null);
    }

    public String label(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;
        String lower = normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String enumish(String text, String fallback) {
        String normalized = text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) return fallback;
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }
}
