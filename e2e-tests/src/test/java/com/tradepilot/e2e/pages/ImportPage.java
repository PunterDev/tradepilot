package com.tradepilot.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ImportPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By account = By.cssSelector("[data-testid='import-account-select']");
    private final By file = By.cssSelector("[data-testid='import-file-input']");
    private final By confirm = By.cssSelector("[data-testid='import-confirm-button']");
    private final By broker = By.cssSelector("[data-testid='import-broker']");
    private final By tradeCount = By.cssSelector("[data-testid='import-trade-count']");
    private final By cancelledIgnored = By.cssSelector("[data-testid='import-cancelled-ignored']");
    private final By duplicateCount = By.cssSelector("[data-testid='import-duplicate-count']");

    public ImportPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        wait.until(ExpectedConditions.presenceOfElementLocated(file));
    }

    public ImportPage selectFirstImportableAccount() {
        Select select = new Select(wait.until(ExpectedConditions.elementToBeClickable(account)));
        select.getOptions().stream()
                .filter(option -> !option.getAttribute("value").isBlank() && !"ALL".equals(option.getAttribute("value")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No importable trading account option was available. Seed or create a trading account first."))
                .click();
        return this;
    }

    public ImportPage upload(String absolutePath) {
        driver.findElement(file).sendKeys(absolutePath);
        wait.until(ExpectedConditions.visibilityOfElementLocated(tradeCount));
        return this;
    }

    public void confirm() {
        wait.until(ExpectedConditions.elementToBeClickable(confirm)).click();
    }

    public String broker() { return driver.findElement(broker).getText(); }
    public int tradeCount() { return firstInt(driver.findElement(tradeCount).getText()); }
    public int cancelledIgnored() { return firstInt(driver.findElement(cancelledIgnored).getText()); }
    public int duplicateCount() { return firstInt(driver.findElement(duplicateCount).getText()); }

    private int firstInt(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("No integer found in text: " + text);
        }
        return Integer.parseInt(matcher.group());
    }
}
