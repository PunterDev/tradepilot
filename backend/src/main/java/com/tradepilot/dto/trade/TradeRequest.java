package com.tradepilot.dto.trade;

import com.tradepilot.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TradeRequest {
    @NotBlank
    private String symbol;
    @NotNull
    private String market;
    @NotNull
    private LocalDate tradeDate;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    @NotNull
    private String side;
    @NotNull
    private SetupType setup;
    @NotNull
    private SessionType session;
    private BigDecimal entryPrice;
    private BigDecimal stopPrice;
    private BigDecimal targetPrice;
    private BigDecimal exitPrice;
    private Integer quantity;
    private BigDecimal riskAmount;
    private BigDecimal pnlAmount;
    private BigDecimal pnlR;
    @NotNull
    private OutcomeType outcome;
    @Min(1)
    @Max(5)
    private Integer confidenceRating;
    @Min(1)
    @Max(5)
    private Integer executionRating;
    private Boolean followedPlan;
    private MistakeTag mistakeTag;
    private String notes;
    private String psychologyNotes;
    private String lessonLearned;
    private String screenshotUrl;
}
