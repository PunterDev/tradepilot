package com.tradepilot.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AppShellPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By logoutButton = By.cssSelector("[data-testid='app-logout-button']");

    public AppShellPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public LoginPage logout() {
        wait.until(ExpectedConditions.elementToBeClickable(logoutButton)).click();
        return new LoginPage(driver, wait);
    }
}
