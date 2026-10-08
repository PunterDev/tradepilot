package com.tradepilot.manualbacktest.entity;

import com.tradepilot.marketdata.model.MarketInstrument;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "manual_backtest_sessions")
public class ManualBacktestSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private MarketInstrument symbol;
    @Column(name = "continuous_symbol", nullable = false, length = 20) private String continuousSymbol;
    @Column(name = "trading_date", nullable = false) private LocalDate tradingDate;
    @Column(nullable = false, length = 64) private String timezone;
    @Column(name = "start_time", nullable = false) private Instant startTime;
    @Column(name = "end_time", nullable = false) private Instant endTime;
    @Column(name = "cursor_ts", nullable = false) private Instant cursorTs;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Enumerated(EnumType.STRING) @Column(name = "risk_profile", nullable = false, length = 32) private RiskProfile riskProfile;
    @Column(columnDefinition = "text") private String notes;
    @Version private Long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "completed_at") private Instant completedAt;

    public enum Status { ACTIVE, COMPLETED, ABANDONED }
    public enum RiskProfile { UNIFORM_1R, PLAYBOOK_WEEKDAY }
    public Long getId() { return id; } public Long getUserId() { return userId; } public void setUserId(Long value) { userId = value; }
    public MarketInstrument getSymbol() { return symbol; } public void setSymbol(MarketInstrument value) { symbol = value; }
    public String getContinuousSymbol() { return continuousSymbol; } public void setContinuousSymbol(String value) { continuousSymbol = value; }
    public LocalDate getTradingDate() { return tradingDate; } public void setTradingDate(LocalDate value) { tradingDate = value; }
    public String getTimezone() { return timezone; } public void setTimezone(String value) { timezone = value; }
    public Instant getStartTime() { return startTime; } public void setStartTime(Instant value) { startTime = value; }
    public Instant getEndTime() { return endTime; } public void setEndTime(Instant value) { endTime = value; }
    public Instant getCursorTs() { return cursorTs; } public void setCursorTs(Instant value) { cursorTs = value; }
    public Status getStatus() { return status; } public void setStatus(Status value) { status = value; }
    public RiskProfile getRiskProfile() { return riskProfile; } public void setRiskProfile(RiskProfile value) { riskProfile = value; }
    public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
    public Long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant value) { updatedAt = value; }
    public Instant getCompletedAt() { return completedAt; } public void setCompletedAt(Instant value) { completedAt = value; }
}
