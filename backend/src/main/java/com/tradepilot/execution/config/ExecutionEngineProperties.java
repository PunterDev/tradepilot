package com.tradepilot.execution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "tradepilot.execution")
public class ExecutionEngineProperties {
    private int cleanExecutionThreshold = 75;
    private int fastReentryMinutes = 7;
    private BigDecimal fastReentryBaselineRatio = BigDecimal.valueOf(0.50);
    private BigDecimal riskIncreasePercent = BigDecimal.valueOf(25);
    private BigDecimal sizeIncreasePercent = BigDecimal.valueOf(25);
    private int executionDropPoints = 15;
    private int minimumPressurePointSamples = 8;
    private int minimumPatternOccurrences = 3;
    private int lookbackDays = 90;
    private int lowEvidenceCoverageThreshold = 25;
    private int moderateEvidenceCoverageThreshold = 50;
    private int highEvidenceCoverageThreshold = 75;

    public int getCleanExecutionThreshold() { return cleanExecutionThreshold; }
    public void setCleanExecutionThreshold(int cleanExecutionThreshold) { this.cleanExecutionThreshold = cleanExecutionThreshold; }
    public int getFastReentryMinutes() { return fastReentryMinutes; }
    public void setFastReentryMinutes(int fastReentryMinutes) { this.fastReentryMinutes = fastReentryMinutes; }
    public BigDecimal getFastReentryBaselineRatio() { return fastReentryBaselineRatio; }
    public void setFastReentryBaselineRatio(BigDecimal fastReentryBaselineRatio) { this.fastReentryBaselineRatio = fastReentryBaselineRatio; }
    public BigDecimal getRiskIncreasePercent() { return riskIncreasePercent; }
    public void setRiskIncreasePercent(BigDecimal riskIncreasePercent) { this.riskIncreasePercent = riskIncreasePercent; }
    public BigDecimal getSizeIncreasePercent() { return sizeIncreasePercent; }
    public void setSizeIncreasePercent(BigDecimal sizeIncreasePercent) { this.sizeIncreasePercent = sizeIncreasePercent; }
    public int getExecutionDropPoints() { return executionDropPoints; }
    public void setExecutionDropPoints(int executionDropPoints) { this.executionDropPoints = executionDropPoints; }
    public int getMinimumPressurePointSamples() { return minimumPressurePointSamples; }
    public void setMinimumPressurePointSamples(int minimumPressurePointSamples) { this.minimumPressurePointSamples = minimumPressurePointSamples; }
    public int getMinimumPatternOccurrences() { return minimumPatternOccurrences; }
    public void setMinimumPatternOccurrences(int minimumPatternOccurrences) { this.minimumPatternOccurrences = minimumPatternOccurrences; }
    public int getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(int lookbackDays) { this.lookbackDays = lookbackDays; }
    public int getLowEvidenceCoverageThreshold() { return lowEvidenceCoverageThreshold; }
    public void setLowEvidenceCoverageThreshold(int lowEvidenceCoverageThreshold) { this.lowEvidenceCoverageThreshold = lowEvidenceCoverageThreshold; }
    public int getModerateEvidenceCoverageThreshold() { return moderateEvidenceCoverageThreshold; }
    public void setModerateEvidenceCoverageThreshold(int moderateEvidenceCoverageThreshold) { this.moderateEvidenceCoverageThreshold = moderateEvidenceCoverageThreshold; }
    public int getHighEvidenceCoverageThreshold() { return highEvidenceCoverageThreshold; }
    public void setHighEvidenceCoverageThreshold(int highEvidenceCoverageThreshold) { this.highEvidenceCoverageThreshold = highEvidenceCoverageThreshold; }
}
