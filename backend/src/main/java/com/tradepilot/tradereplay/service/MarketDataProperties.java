package com.tradepilot.tradereplay.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "tradepilot.market-data")
public class MarketDataProperties {
    private String provider = "disabled";
    private Databento databento = new Databento();
    private int replayDelayHours = 24;
    private int preTradeMinutes = 15;
    private int postTradeMinutes = 30;
    private int maxWindowHours = 2;
    private boolean workerEnabled = false;
    private long workerInitialDelayMs = 60_000;
    private long workerFixedDelayMs = 300_000;
    private boolean cacheEnabled = true;
    private long staleProcessingTimeoutMinutes = 5;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Databento getDatabento() { return databento; }
    public void setDatabento(Databento databento) { this.databento = databento; }
    public int getReplayDelayHours() { return replayDelayHours; }
    public void setReplayDelayHours(int replayDelayHours) { this.replayDelayHours = replayDelayHours; }
    public int getPreTradeMinutes() { return preTradeMinutes; }
    public void setPreTradeMinutes(int preTradeMinutes) { this.preTradeMinutes = preTradeMinutes; }
    public int getPostTradeMinutes() { return postTradeMinutes; }
    public void setPostTradeMinutes(int postTradeMinutes) { this.postTradeMinutes = postTradeMinutes; }
    public int getMaxWindowHours() { return maxWindowHours; }
    public void setMaxWindowHours(int maxWindowHours) { this.maxWindowHours = maxWindowHours; }
    public boolean isWorkerEnabled() { return workerEnabled; }
    public void setWorkerEnabled(boolean workerEnabled) { this.workerEnabled = workerEnabled; }
    public long getWorkerInitialDelayMs() { return workerInitialDelayMs; }
    public void setWorkerInitialDelayMs(long workerInitialDelayMs) { this.workerInitialDelayMs = workerInitialDelayMs; }
    public long getWorkerFixedDelayMs() { return workerFixedDelayMs; }
    public void setWorkerFixedDelayMs(long workerFixedDelayMs) { this.workerFixedDelayMs = workerFixedDelayMs; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    public long getStaleProcessingTimeoutMinutes() { return staleProcessingTimeoutMinutes; }
    public void setStaleProcessingTimeoutMinutes(long staleProcessingTimeoutMinutes) { this.staleProcessingTimeoutMinutes = staleProcessingTimeoutMinutes; }

    public static class Databento {
        private String apiKey = "";
        private String dataset = "GLBX.MDP3";
        private String schema = "ohlcv-1m";
        private boolean costCheckEnabled = true;
        private BigDecimal maxRequestCostUsd = new BigDecimal("0.05");

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getDataset() { return dataset; }
        public void setDataset(String dataset) { this.dataset = dataset; }
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public boolean isCostCheckEnabled() { return costCheckEnabled; }
        public void setCostCheckEnabled(boolean costCheckEnabled) { this.costCheckEnabled = costCheckEnabled; }
        public BigDecimal getMaxRequestCostUsd() { return maxRequestCostUsd; }
        public void setMaxRequestCostUsd(BigDecimal maxRequestCostUsd) { this.maxRequestCostUsd = maxRequestCostUsd; }
    }
}
