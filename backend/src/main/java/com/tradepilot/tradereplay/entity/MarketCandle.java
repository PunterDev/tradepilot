package com.tradepilot.tradereplay.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_candles",
        indexes = {@Index(name = "idx_market_candles_lookup", columnList = "provider, contract_symbol, candle_interval, candle_timestamp")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_market_candles_provider_contract_interval_ts", columnNames = {"provider", "contract_symbol", "candle_interval", "candle_timestamp"})})
public class MarketCandle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String provider;
    @Column(name = "contract_symbol", nullable = false, length = 40)
    private String contractSymbol;
    @Column(name = "candle_interval", nullable = false, length = 20)
    private String interval;
    @Column(name = "candle_timestamp", nullable = false)
    private Instant timestamp;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal open;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal high;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal low;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal close;
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal volume;

    public Long getId() { return id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getContractSymbol() { return contractSymbol; }
    public void setContractSymbol(String contractSymbol) { this.contractSymbol = contractSymbol; }
    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
}
