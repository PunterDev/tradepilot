package com.tradepilot.manualbacktest.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "manual_backtest_trade_events")
public class ManualBacktestTradeEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trade_id", nullable = false) private Long tradeId;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "replay_cursor_ts", nullable = false) private Instant replayCursorTs;
    @Column(name = "event_type", nullable = false, length = 40) private String eventType;
    @Column(name = "old_price", precision = 16, scale = 6) private BigDecimal oldPrice;
    @Column(name = "new_price", precision = 16, scale = 6) private BigDecimal newPrice;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    public void record(Long trade, Long session, Long user, Instant cursor, String type, BigDecimal oldValue, BigDecimal newValue) { tradeId=trade; sessionId=session; userId=user; replayCursorTs=cursor; eventType=type; oldPrice=oldValue; newPrice=newValue; createdAt=Instant.now(); }
}
