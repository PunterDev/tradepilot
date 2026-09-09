package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.LoginPage;

@Tag("e2e")
class LoginTest extends BaseTest {
    @Test
    void validUserCanLogin() {
        DashboardPage dashboard = new LoginPage(driver, wait)
            .open(baseUrl)
            .login(testData.proUser().email(), testData.proUser().password());

        assertTrue(dashboard.isDisplayed(), "Dashboard should be visible after login");
        assertFalse(dashboard.currentPath().equals("/login"), "Valid login should navigate away from /login");
    }

    @Test
    void invalidCredentialsShowUserFacingErrorAndRemainUnauthenticated() {
        LoginPage login = new LoginPage(driver, wait)
                .open(baseUrl)
                .submitInvalid("selenium-invalid@example.test", "not-the-password");

        String error = login.getError();
        assertFalse(error.isBlank(), "Invalid login should display a user-facing error");
        assertTrue(login.isUnauthenticated(), "Invalid login must not persist a JWT");
    }
}
