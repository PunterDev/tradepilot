package com.tradepilot.dto.ai;

import lombok.Data;

@Data
public class TradeSnapshot {
    private Long id;
    private String symbol;
    private String side;
    private Double pnl;
    private Integer quantity;
    private Double entryPrice;
    private Double exitPrice;
    private String entryTime;
    private String exitTime;
    private String notes;
}