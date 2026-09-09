package com.tradepilot.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By email = By.cssSelector("[data-testid='login-email']");
    private final By password = By.cssSelector("[data-testid='login-password']");
    private final By submit = By.cssSelector("[data-testid='login-submit']");
    private final By error = By.cssSelector("[data-testid='login-error']");
    private final By message = By.cssSelector("[data-testid='auth-message']");

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(email));
        return this;
    }

    public DashboardPage login(String username, String secret) {
        fillCredentials(username, secret);
        driver.findElement(submit).click();
        return new DashboardPage(driver, wait);
    }

    public LoginPage submitInvalid(String username, String secret) {
        fillCredentials(username, secret);
        driver.findElement(submit).click();
        return this;
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(email)).isDisplayed();
    }

    public String getError() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(error)).getText();
    }

    public String getMessage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(message)).getText();
    }

    public boolean isUnauthenticated() {
        wait.until((ExpectedCondition<Boolean>) webDriver ->
                ((String) ((JavascriptExecutor) webDriver)
                        .executeScript("return window.localStorage.getItem('tradepilot_token') || window.sessionStorage.getItem('tradepilot_token');")) == null);
        return true;
    }

    private void fillCredentials(String username, String secret) {
        WebElement emailInput = driver.findElement(email);
        WebElement passwordInput = driver.findElement(password);
        emailInput.clear();
        passwordInput.clear();
        emailInput.sendKeys(username);
        passwordInput.sendKeys(secret);
    }
}
