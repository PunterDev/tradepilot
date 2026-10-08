package com.tradepilot.marketdata.entity;

import com.tradepilot.marketdata.model.MarketInstrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_bars", uniqueConstraints = @UniqueConstraint(name = "uk_market_bar", columnNames = {"continuous_symbol", "timeframe", "ts_event"}))
public class MarketBar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MarketInstrument symbol;

    @Column(name = "continuous_symbol", nullable = false, length = 20)
    private String continuousSymbol;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(nullable = false, length = 10)
    private String timeframe;

    @Column(name = "ts_event", nullable = false)
    private Instant tsEvent;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketBar() {
    }

    public MarketBar(MarketInstrument symbol, String continuousSymbol, String source, Long instrumentId, String timeframe, Instant tsEvent,
                     BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume, Instant createdAt) {
        this.symbol = symbol;
        this.continuousSymbol = continuousSymbol;
        this.source = source;
        this.instrumentId = instrumentId;
        this.timeframe = timeframe;
        this.tsEvent = tsEvent;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public MarketInstrument getSymbol() { return symbol; }
    public String getContinuousSymbol() { return continuousSymbol; }
    public String getSource() { return source; }
    public Long getInstrumentId() { return instrumentId; }
    public String getTimeframe() { return timeframe; }
    public Instant getTsEvent() { return tsEvent; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public Long getVolume() { return volume; }
    public Instant getCreatedAt() { return createdAt; }
}
