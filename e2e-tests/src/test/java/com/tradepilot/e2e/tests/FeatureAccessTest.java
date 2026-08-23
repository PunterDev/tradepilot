package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.LoginPage;

@Tag("e2e")
class FeatureAccessTest extends BaseTest {
    @Test
    void freeUserSeesUpgradePromptForProFeature() {
        String email = requiredEnv("TRADEPILOT_FREE_TEST_EMAIL");
        String password = requiredEnv("TRADEPILOT_FREE_TEST_PASSWORD");

        new LoginPage(driver, wait)
                .open(baseUrl)
                .login(email, password);

        driver.get(baseUrl + "/session-review");

        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='pro-feature-upgrade-prompt']"))).isDisplayed(),
                "Free user should see the Pro upgrade/access restriction prompt");
    }

    @Test
    void proUserCanAccessSameFeature() {
        String email = requiredEnv("TRADEPILOT_TEST_EMAIL");
        String password = requiredEnv("TRADEPILOT_TEST_PASSWORD");

        new LoginPage(driver, wait)
                .open(baseUrl)
                .login(email, password);

        driver.get(baseUrl + "/session-review");

        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='session-review-heading']"))).isDisplayed(),
                "Pro user should see the Session Review feature page");
    }
}
