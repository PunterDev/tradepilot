package com.tradepilot.coach.service;

import com.tradepilot.coach.dto.CoachAiResponse;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Slf4j
public class CoachOutputConsistencyService {

    public CoachAiResponse enforce(
            CoachSnapshotType type,
            CoachDeterministicSummary summary,
            CoachAiResponse response,
            Long userId,
            Long accountId,
            LocalDate reviewDate
    ) {
        if (response == null || summary == null || !hasPositiveOutcome(summary)) {
            return response;
        }

        String dominantDirection = dominantDirection(summary);
        if (dominantDirection == null) {
            return response;
        }

        String patchedText = profitableDirectionReview(dominantDirection);
        response.setNextSessionFocus(patchIfContradictory(
                response.getNextSessionFocus(),
                patchedText,
                type,
                summary,
                userId,
                accountId,
                reviewDate,
                "nextSessionFocus"
        ));
        response.setAvoidNextSession(patchIfContradictory(
                response.getAvoidNextSession(),
                processReviewText(type, summary),
                type,
                summary,
                userId,
                accountId,
                reviewDate,
                "avoidNextSession"
        ));
        return response;
    }

    private boolean hasPositiveOutcome(CoachDeterministicSummary summary) {
        return summary.getNetPnl() != null && summary.getNetPnl().compareTo(BigDecimal.ZERO) > 0;
    }

    private String dominantDirection(CoachDeterministicSummary summary) {
        int longCount = summary.getLongTradeCount() == null ? 0 : summary.getLongTradeCount();
        int shortCount = summary.getShortTradeCount() == null ? 0 : summary.getShortTradeCount();
        int unknownCount = summary.getUnknownTradeCount() == null ? 0 : summary.getUnknownTradeCount();
        if (longCount <= 0 && shortCount <= 0) {
            return null;
        }
        if (longCount > 0 && shortCount == 0 && unknownCount == 0) {
            return "LONG";
        }
        if (shortCount > 0 && longCount == 0 && unknownCount == 0) {
            return "SHORT";
        }
        if (longCount > shortCount && pnlPositive(summary.getLongPnl())) {
            return "LONG";
        }
        if (shortCount > longCount && pnlPositive(summary.getShortPnl())) {
            return "SHORT";
        }
        return null;
    }

    private boolean pnlPositive(BigDecimal pnl) {
        return pnl != null && pnl.compareTo(BigDecimal.ZERO) > 0;
    }

    private String patchIfContradictory(
            String original,
            String replacement,
            CoachSnapshotType type,
            CoachDeterministicSummary summary,
            Long userId,
            Long accountId,
            LocalDate reviewDate,
            String field
    ) {
        if (!contradictsProfitableDirection(original, dominantDirection(summary))) {
            return original;
        }
        log.warn(
                "AI Coach consistency patch applied. userId={}, accountId={}, reviewDate={}, type={}, field={}, reason={}, originalText={}",
                userId,
                accountId,
                reviewDate,
                type,
                field,
                "PROFITABLE_DIRECTION_DESCRIBED_AS_FAILED",
                sanitizeForLog(original)
        );
        return replacement;
    }

    boolean contradictsProfitableDirection(String text, String dominantDirection) {
        if (text == null || text.isBlank() || dominantDirection == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if ("LONG".equals(dominantDirection)) {
            return containsAny(normalized,
                    "long idea failed",
                    "long failed",
                    "long direction failed",
                    "bullish idea failed",
                    "bullish failed",
                    "bullish direction failed"
            );
        }
        if ("SHORT".equals(dominantDirection)) {
            return containsAny(normalized,
                    "short idea failed",
                    "short failed",
                    "short direction failed",
                    "bearish idea failed",
                    "bearish failed",
                    "bearish direction failed"
            );
        }
        return false;
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String profitableDirectionReview(String side) {
        return "The " + side
                + " trade was profitable. Review whether the entry had a clear thesis, invalidation point, predefined stop, and realistic target before taking a similar setup.";
    }

    private String processReviewText(CoachSnapshotType type, CoachDeterministicSummary summary) {
        if (type == CoachSnapshotType.DAILY && summary.getTotalTrades() != null && summary.getTotalTrades() == 1) {
            return "This was not an overtrading day. Keep the review focused on process quality: thesis, invalidation, stop, and target.";
        }
        if (type == CoachSnapshotType.WEEKLY) {
            return "Keep weekly process critique separate from factual outcome. Review whether the profitable direction still met your risk and setup standards.";
        }
        return "Keep process critique separate from factual outcome. Review whether the profitable direction still met your entry and risk standards.";
    }

    private String sanitizeForLog(String text) {
        if (text == null) {
            return null;
        }
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() > 240 ? singleLine.substring(0, 240) + "..." : singleLine;
    }
}
