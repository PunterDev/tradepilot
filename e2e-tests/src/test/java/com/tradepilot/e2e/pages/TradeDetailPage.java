package com.tradepilot.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TradeDetailPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By modal = By.cssSelector("[data-testid='trade-detail-modal']");
    private final By generateReview = By.cssSelector("[data-testid='generate-trade-review']");
    private final By memoryStatus = By.cssSelector("[data-testid='trading-memory-status']");
    private final By similarTrades = By.cssSelector("[data-testid^='similar-trade-row-']");
    private final By symbol = By.cssSelector("[data-testid='trade-detail-symbol']");
    private final By side = By.cssSelector("[data-testid='trade-detail-side']");
    private final By entry = By.cssSelector("[data-testid='trade-detail-entry']");
    private final By exit = By.cssSelector("[data-testid='trade-detail-exit']");
    private final By pnl = By.cssSelector("[data-testid='trade-detail-pnl']");

    public TradeDetailPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        wait.until(ExpectedConditions.visibilityOfElementLocated(modal));
    }

    public TradeDetailPage generateReview() {
        wait.until(ExpectedConditions.elementToBeClickable(generateReview)).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(memoryStatus));
        return this;
    }

    public String waitForFinalMemoryStatus() {
        return wait.until(webDriver -> {
            String status = memoryStatus();
            return switch (status) {
                case "READY", "FAILED", "NOT_ENOUGH_DATA" -> status;
                default -> null;
            };
        });
    }

    public String memoryStatus() {
        String value = driver.findElement(memoryStatus).getAttribute("data-status");
        return value == null ? "" : value.trim().toUpperCase();
    }

    public int similarTradeCount() {
        return driver.findElements(similarTrades).size();
    }

    public String symbol() { return wait.until(ExpectedConditions.visibilityOfElementLocated(symbol)).getText(); }
    public String side() { return wait.until(ExpectedConditions.visibilityOfElementLocated(side)).getText(); }
    public String entry() { return wait.until(ExpectedConditions.visibilityOfElementLocated(entry)).getText(); }
    public String exit() { return wait.until(ExpectedConditions.visibilityOfElementLocated(exit)).getText(); }
    public String pnl() { return wait.until(ExpectedConditions.visibilityOfElementLocated(pnl)).getText(); }

    public long openFirstSimilarTradeAndReturnId() {
        var row = wait.until(ExpectedConditions.elementToBeClickable(similarTrades));
        String testId = row.getAttribute("data-testid");
        row.click();
        return Long.parseLong(testId.replace("similar-trade-row-", ""));
    }
}
