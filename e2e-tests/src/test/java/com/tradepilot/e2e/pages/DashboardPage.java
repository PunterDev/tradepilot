package com.tradepilot.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DashboardPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By heading = By.cssSelector("[data-testid='dashboard-heading']");

    public DashboardPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(heading)).isDisplayed();
    }

    public ImportPage importPanel() {
        return new ImportPage(driver, wait);
    }

    public TradeDetailPage openKnownTrade(long tradeId) {
        driver.get(java.net.URI.create(driver.getCurrentUrl()).resolve("/trades/" + tradeId).toString());
        return new TradeDetailPage(driver, wait);
    }

    public String currentPath() {
        return java.net.URI.create(driver.getCurrentUrl()).getPath();
    }
}
