package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.AppShellPage;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.LoginPage;

@Tag("e2e")
class AuthenticationLifecycleTest extends BaseTest {
    @Test
    void unauthenticatedUserIsRedirectedFromProtectedRoute() {
        driver.get(baseUrl + "/dashboard");

        LoginPage login = new LoginPage(driver, wait);

        assertTrue(login.isDisplayed(), "Protected routes should redirect unauthenticated users to login");
        assertEquals("/login", currentPath(), "Unauthenticated protected route access should land on /login");
        assertTrue(login.isUnauthenticated(), "Route guard must not create an authenticated session");
    }

    @Test
    void validLoginPersistsAuthenticatedSessionAcrossRefresh() {
        DashboardPage dashboard = new LoginPage(driver, wait)
                .open(baseUrl)
                .login(testData.proUser().email(), testData.proUser().password());

        assertTrue(dashboard.isDisplayed(), "Dashboard should be visible after valid login");

        driver.navigate().refresh();

        assertTrue(new DashboardPage(driver, wait).isDisplayed(), "Authenticated session should survive browser refresh");
        assertEquals("/dashboard", currentPath(), "Refresh should preserve the authenticated dashboard route");
    }

    @Test
    void logoutClearsSessionAndProtectedRoutesAreBlockedAgain() {
        DashboardPage dashboard = new LoginPage(driver, wait)
                .open(baseUrl)
                .login(testData.proUser().email(), testData.proUser().password());
        assertTrue(dashboard.isDisplayed(), "Dashboard should be visible before logout");

        LoginPage login = new AppShellPage(driver, wait).logout();

        assertTrue(login.isDisplayed(), "Logout should return the user to login");
        assertEquals("/login", currentPath(), "Logout should navigate to /login");
        assertTrue(login.isUnauthenticated(), "Logout should clear persisted auth storage");

        driver.get(baseUrl + "/dashboard");

        assertTrue(new LoginPage(driver, wait).isDisplayed(), "Protected routes should remain blocked after logout");
        assertEquals("/login", currentPath(), "Protected route access after logout should redirect to /login");
    }

    @Test
    void invalidStoredTokenIsRejectedAndCleared() {
        driver.get(baseUrl + "/login");
        ((JavascriptExecutor) driver).executeScript(
                "window.localStorage.clear();"
                        + "window.sessionStorage.clear();"
                        + "window.localStorage.setItem('tradepilot_token', 'selenium-invalid-token');"
                        + "window.localStorage.setItem('tradepilot_user', arguments[0]);",
                "{\"id\":999999,\"name\":\"Stale E2E User\",\"email\":\"stale-selenium-e2e@tradepilot.test\",\"plan\":\"PRO\",\"role\":\"USER\"}"
        );

        driver.get(baseUrl + "/dashboard");
        LoginPage login = new LoginPage(driver, wait);

        assertTrue(login.isDisplayed(), "Invalid stored JWT should be rejected and return to login");
        assertTrue(login.getMessage().contains("Your session expired"), "Invalid stored JWT should show an expired-session message");
        assertTrue(login.isUnauthenticated(), "Invalid stored JWT should be removed from auth storage");
        assertEquals("/login", currentPath(), "Invalid stored JWT should not remain on a protected route");
    }

    @Test
    void unverifiedE2eUserCannotLogin() {
        LoginPage login = new LoginPage(driver, wait)
                .open(baseUrl)
                .submitInvalid(testData.unverifiedUser().email(), testData.unverifiedUser().password());

        assertTrue(login.getError().contains("Please verify your email"), "Unverified users should see verification guidance");
        assertTrue(login.isUnauthenticated(), "Unverified login attempt must not persist a JWT");
        assertFalse(currentPath().equals("/dashboard"), "Unverified login must not enter the authenticated app");
    }

    private String currentPath() {
        return URI.create(driver.getCurrentUrl()).getPath();
    }
}
