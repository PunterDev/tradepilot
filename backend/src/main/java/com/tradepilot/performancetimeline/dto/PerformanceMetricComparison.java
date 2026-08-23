package com.tradepilot.performancetimeline.dto;

import com.tradepilot.performancetimeline.entity.ComparisonResult;

import java.math.BigDecimal;

public class PerformanceMetricComparison {
    private String metric;
    private BigDecimal previousValue;
    private BigDecimal currentValue;
    private BigDecimal absoluteChange;
    private BigDecimal percentageChange;
    private ComparisonResult direction;
    private boolean higherIsBetter;

    public PerformanceMetricComparison() {
    }

    public PerformanceMetricComparison(String metric, BigDecimal previousValue, BigDecimal currentValue, BigDecimal absoluteChange, BigDecimal percentageChange, ComparisonResult direction, boolean higherIsBetter) {
        this.metric = metric;
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
        this.direction = direction;
        this.higherIsBetter = higherIsBetter;
    }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public BigDecimal getPreviousValue() { return previousValue; }
    public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public BigDecimal getAbsoluteChange() { return absoluteChange; }
    public void setAbsoluteChange(BigDecimal absoluteChange) { this.absoluteChange = absoluteChange; }
    public BigDecimal getPercentageChange() { return percentageChange; }
    public void setPercentageChange(BigDecimal percentageChange) { this.percentageChange = percentageChange; }
    public ComparisonResult getDirection() { return direction; }
    public void setDirection(ComparisonResult direction) { this.direction = direction; }
    public boolean isHigherIsBetter() { return higherIsBetter; }
    public void setHigherIsBetter(boolean higherIsBetter) { this.higherIsBetter = higherIsBetter; }
}
