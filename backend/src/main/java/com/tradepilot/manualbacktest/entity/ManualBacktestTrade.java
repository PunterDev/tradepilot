package com.tradepilot.manualbacktest.entity;

import com.tradepilot.marketdata.model.MarketInstrument;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "manual_backtest_trades")
public class ManualBacktestTrade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private MarketInstrument symbol;
    @Column(name = "instrument_id") private Long instrumentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Direction direction;
    @Column(name = "order_placed_ts", nullable = false) private Instant orderPlacedTs;
    @Enumerated(EnumType.STRING) @Column(name = "entry_order_type", nullable = false, length = 10) private EntryOrderType entryOrderType;
    @Column(name = "configured_entry_price", precision = 16, scale = 6) private BigDecimal configuredEntryPrice;
    @Column(name = "entry_ts") private Instant entryTs;
    @Column(name = "entry_price", precision = 16, scale = 6) private BigDecimal entryPrice;
    @Column(name = "initial_stop_price", nullable = false, precision = 16, scale = 6) private BigDecimal initialStopPrice;
    @Column(name = "current_stop_price", nullable = false, precision = 16, scale = 6) private BigDecimal currentStopPrice;
    @Column(name = "target_price", nullable = false, precision = 16, scale = 6) private BigDecimal targetPrice;
    @Column(name = "exit_ts") private Instant exitTs;
    @Column(name = "exit_price", precision = 16, scale = 6) private BigDecimal exitPrice;
    @Enumerated(EnumType.STRING) @Column(name = "exit_reason", length = 20) private ExitReason exitReason;
    @Column(name = "initial_risk_points", precision = 16, scale = 6) private BigDecimal initialRiskPoints;
    @Column(name = "raw_r", precision = 16, scale = 6) private BigDecimal rawR;
    @Column(name = "risk_multiplier", nullable = false, precision = 8, scale = 4) private BigDecimal riskMultiplier;
    @Column(name = "weighted_r", precision = 16, scale = 6) private BigDecimal weightedR;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "pending_action", length = 20) private String pendingAction;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public enum Direction { LONG, SHORT } public enum EntryOrderType { MARKET, LIMIT, STOP } public enum Status { PENDING, OPEN, CLOSED, CANCELLED } public enum ExitReason { STOP, TARGET, MANUAL, SESSION_END }
    public Long getId(){return id;} public Long getSessionId(){return sessionId;} public void setSessionId(Long value){sessionId=value;} public Long getUserId(){return userId;} public void setUserId(Long value){userId=value;}
    public MarketInstrument getSymbol(){return symbol;} public void setSymbol(MarketInstrument value){symbol=value;} public Long getInstrumentId(){return instrumentId;} public void setInstrumentId(Long value){instrumentId=value;}
    public Direction getDirection(){return direction;} public void setDirection(Direction value){direction=value;} public Instant getOrderPlacedTs(){return orderPlacedTs;} public void setOrderPlacedTs(Instant value){orderPlacedTs=value;}
    public EntryOrderType getEntryOrderType(){return entryOrderType;} public void setEntryOrderType(EntryOrderType value){entryOrderType=value;} public BigDecimal getConfiguredEntryPrice(){return configuredEntryPrice;} public void setConfiguredEntryPrice(BigDecimal value){configuredEntryPrice=value;}
    public Instant getEntryTs(){return entryTs;} public void setEntryTs(Instant value){entryTs=value;} public BigDecimal getEntryPrice(){return entryPrice;} public void setEntryPrice(BigDecimal value){entryPrice=value;}
    public BigDecimal getInitialStopPrice(){return initialStopPrice;} public void setInitialStopPrice(BigDecimal value){initialStopPrice=value;} public BigDecimal getCurrentStopPrice(){return currentStopPrice;} public void setCurrentStopPrice(BigDecimal value){currentStopPrice=value;}
    public BigDecimal getTargetPrice(){return targetPrice;} public void setTargetPrice(BigDecimal value){targetPrice=value;} public Instant getExitTs(){return exitTs;} public void setExitTs(Instant value){exitTs=value;} public BigDecimal getExitPrice(){return exitPrice;} public void setExitPrice(BigDecimal value){exitPrice=value;}
    public ExitReason getExitReason(){return exitReason;} public void setExitReason(ExitReason value){exitReason=value;} public BigDecimal getInitialRiskPoints(){return initialRiskPoints;} public void setInitialRiskPoints(BigDecimal value){initialRiskPoints=value;}
    public BigDecimal getRawR(){return rawR;} public void setRawR(BigDecimal value){rawR=value;} public BigDecimal getRiskMultiplier(){return riskMultiplier;} public void setRiskMultiplier(BigDecimal value){riskMultiplier=value;}
    public BigDecimal getWeightedR(){return weightedR;} public void setWeightedR(BigDecimal value){weightedR=value;} public Status getStatus(){return status;} public void setStatus(Status value){status=value;}
    public String getPendingAction(){return pendingAction;} public void setPendingAction(String value){pendingAction=value;}
    public String getNotes(){return notes;} public void setNotes(String value){notes=value;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant value){createdAt=value;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant value){updatedAt=value;}
}
