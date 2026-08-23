package com.tradepilot.performancetimeline.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PerformanceTimelineHashService {
    private static final String CALCULATION_VERSION = "performance-timeline-v1";
    private static final String PROMPT_VERSION = "journey-prompt-v1";

    public String hash(Long accountId, LocalDate periodStart, LocalDate periodEnd, List<Trade> trades, TradingAccount account) {
        StringBuilder raw = new StringBuilder(CALCULATION_VERSION)
                .append('|').append(PROMPT_VERSION)
                .append('|').append(accountId)
                .append('|').append(periodStart)
                .append('|').append(periodEnd)
                .append('|').append(account == null ? null : account.getDefaultRiskAmount());
        trades.stream()
                .sorted(Comparator.comparing(Trade::getTradeDate).thenComparing(trade -> trade.getId() == null ? 0L : trade.getId()))
                .forEach(trade -> raw.append('|')
                        .append(trade.getId()).append(',')
                        .append(trade.getUpdatedAt()).append(',')
                        .append(trade.getPnlAmount()).append(',')
                        .append(trade.getPnlR()).append(',')
                        .append(trade.getSide()).append(',')
                        .append(trade.getSetup()).append(',')
                        .append(trade.getConfidence()).append(',')
                        .append(trade.getConfidenceRating()).append(',')
                        .append(trade.getEmotion()).append(',')
                        .append(trade.getNotes()).append(',')
                        .append(trade.getRiskAmount()).append(',')
                        .append(trade.getEntryTime()).append(',')
                        .append(trade.getExitTime()));
        return sha256(raw.toString());
    }

    public String calculationVersion() {
        return CALCULATION_VERSION;
    }

    public String promptVersion() {
        return PROMPT_VERSION;
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash performance timeline data", e);
        }
    }
}
