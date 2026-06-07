package com.tradepilot.dto.trade;

import com.tradepilot.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TradeResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private String accountType;
    private String symbol;
    private Market market;
    private LocalDate tradeDate;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Side side;
    private SetupType setup;
    private SessionType session;
    private BigDecimal entryPrice;
    private BigDecimal stopPrice;
    private BigDecimal targetPrice;
    private BigDecimal exitPrice;
    private Integer quantity;
    private BigDecimal riskAmount;
    private BigDecimal pnlAmount;
    private BigDecimal pnlR;
    private OutcomeType outcome;
    private Integer confidence;
    private Integer confidenceRating;
    private String emotion;
    private Integer executionRating;
    private Boolean followedPlan;
    private MistakeTag mistakeTag;
    private String notes;
    private String psychologyNotes;
    private String lessonLearned;
    private String screenshotUrl;
}
