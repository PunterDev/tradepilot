package com.tradepilot.ai.memory.service;

import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.Trade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class TradeMemoryBuilder {
    public static final int MEMORY_VERSION = 1;
    private final TradeMemoryNormalizer normalizer;

    public TradeMemoryBuilder(TradeMemoryNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public TradeMemoryDocument build(Trade trade, AiTradeReview review) {
        List<String> lines = new ArrayList<>();
        add(lines, "Instrument", trade.getSymbol());
        add(lines, "Direction", trade.getSide());
        add(lines, "Setup", trade.getSetup() == null ? null : trade.getSetup().name());
        add(lines, "Session", trade.getSession() == null ? null : trade.getSession().name());
        add(lines, "Entry reasoning", trade.getNotes());
        add(lines, "Emotion", trade.getEmotion());
        add(lines, "Mistake", mistakeSource(trade, review));
        add(lines, "Rule adherence", trade.getFollowedPlan() == null ? null : Boolean.TRUE.equals(trade.getFollowedPlan()) ? "Followed plan" : "Plan not followed");
        add(lines, "Result", formatR(trade));
        add(lines, "PnL", trade.getPnlAmount() == null ? null : "$" + trade.getPnlAmount());
        add(lines, "Hold time", holdTime(trade));
        add(lines, "Trader notes", trade.getNotes());
        add(lines, "Psychology notes", trade.getPsychologyNotes());
        add(lines, "Lesson learned", trade.getLessonLearned());
        add(lines, "AI review summary", review == null ? null : review.getSummary());

        String content = String.join("\n", lines).trim().replaceAll("[ \\t]+", " ");
        String setupSource = trade.getSetup() == null ? null : trade.getSetup().name();
        String emotionSource = trade.getEmotion();
        String mistakeSource = mistakeSource(trade, review);
        return new TradeMemoryDocument(
                MEMORY_VERSION,
                content,
                sha256(MEMORY_VERSION + "\n" + content),
                normalizer.normalizeSetup(setupSource),
                normalizer.normalizeEmotion(emotionSource),
                normalizer.normalizeMistake(mistakeSource),
                setupSource,
                emotionSource,
                mistakeSource,
                summary(trade, review)
        );
    }

    private String mistakeSource(Trade trade, AiTradeReview review) {
        if (trade.getMistakeTag() != null) return trade.getMistakeTag().name();
        if (review != null && review.getTagsJson() != null) return review.getTagsJson();
        if (review != null && review.getMistakesJson() != null) return review.getMistakesJson();
        return null;
    }

    private String summary(Trade trade, AiTradeReview review) {
        if (review != null && review.getSummary() != null && !review.getSummary().isBlank()) {
            return review.getSummary();
        }
        String direction = trade.getSide() == null ? "trade" : trade.getSide().toLowerCase();
        String result = formatR(trade);
        if (result == null && trade.getPnlAmount() != null) result = "$" + trade.getPnlAmount();
        return "%s %s on %s.".formatted(trade.getSymbol(), direction, result == null ? "recorded outcome" : result);
    }

    private String formatR(Trade trade) {
        BigDecimal r = trade.getPnlR();
        if (r == null) return null;
        return (r.signum() > 0 ? "+" : "") + r.setScale(2, RoundingMode.HALF_UP).toPlainString() + "R";
    }

    private String holdTime(Trade trade) {
        if (trade.getEntryTime() == null || trade.getExitTime() == null) return null;
        long minutes = Duration.between(trade.getEntryTime(), trade.getExitTime()).toMinutes();
        return minutes >= 0 ? minutes + " minutes" : null;
    }

    private void add(List<String> lines, String key, String value) {
        if (value == null || value.isBlank()) return;
        lines.add(key + ": " + value.trim().replaceAll("\\s+", " "));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash Trade Memory document.", ex);
        }
    }
}
