package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.LoginPage;

@Tag("e2e")
class FeatureAccessTest extends BaseTest {
    @Test
    void freeUserSeesUpgradePromptForProFeature() {
        DashboardPage dashboard = new LoginPage(driver, wait)
                .open(baseUrl)
                .login(testData.freeUser().email(), testData.freeUser().password());
        assertTrue(dashboard.isDisplayed(), "Free user should land on the dashboard before feature navigation");

        driver.get(baseUrl + "/session-review");

        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='pro-feature-upgrade-prompt']"))).isDisplayed(),
                "Free user should see the Pro upgrade/access restriction prompt");
    }

    @Test
    void proUserCanAccessSameFeature() {
        DashboardPage dashboard = new LoginPage(driver, wait)
                .open(baseUrl)
                .login(testData.proUser().email(), testData.proUser().password());
        assertTrue(dashboard.isDisplayed(), "Pro user should land on the dashboard before feature navigation");

        driver.get(baseUrl + "/session-review");

        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='session-review-heading']"))).isDisplayed(),
                "Pro user should see the Session Review feature page");
    }
}
