package com.tradepilot.discipline;

import com.tradepilot.dto.ai.TradeSnapshot;
import com.tradepilot.dto.discipline.RuleViolationDto;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class DeterministicRuleDetector {
    public List<RuleViolationDto> detect(Long tradeId, TradeSnapshot trade, int dailyTradeCount, double dailyPnl) {
        List<RuleViolationDto> violations = new ArrayList<>();
        if (trade == null || tradeId == null) return violations;

        LocalTime entryTime = parseTime(trade.getEntryTime());

        if (entryTime != null && entryTime.isBefore(LocalTime.of(9, 45))) {
            violations.add(build(tradeId, "NO_TRADE_BEFORE_945", "No trades before 9:45 AM ET",
                    "SESSION", "MAJOR", "Entry time was before your 9:45 AM NY open filter.", "SYSTEM"));
        }

        if (dailyTradeCount > 5) {
            violations.add(build(tradeId, "MAX_TRADES_PER_DAY", "Do not overtrade",
                    "DISCIPLINE", "MODERATE", "Daily trade count exceeded the configured limit of 5 trades.", "SYSTEM"));
        }

        if (dailyPnl <= -1000) {
            violations.add(build(tradeId, "MAX_DAILY_LOSS_BREACH", "Respect max daily loss",
                    "RISK", "CRITICAL", "Daily PnL reached the configured max daily loss threshold.", "SYSTEM"));
        }

        return violations;
    }

    private RuleViolationDto build(Long tradeId, String code, String title, String category, String severity, String reason, String source) {
        return RuleViolationDto.builder()
                .tradeId(tradeId)
                .ruleCode(code)
                .ruleTitle(title)
                .category(category)
                .severity(severity)
                .reason(reason)
                .source(source)
                .build();
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
