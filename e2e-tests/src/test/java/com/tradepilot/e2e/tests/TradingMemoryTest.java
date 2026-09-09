package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.LoginPage;
import com.tradepilot.e2e.pages.TradeDetailPage;

@Tag("ai-integration")
class TradingMemoryTest extends BaseTest {
    @Test
    void reviewedTradeEventuallyShowsReadyMemory() {
        rejectProductionMutation("reviewedTradeEventuallyShowsReadyMemory");
        long tradeId = testData.proUser().tradeId();

        DashboardPage dashboard = new LoginPage(driver, wait)
            .open(baseUrl)
            .login(testData.proUser().email(), testData.proUser().password());

        assertTrue(dashboard.isDisplayed());
        TradeDetailPage trade = dashboard.openKnownTrade(tradeId).generateReview();

        assertEquals("READY", trade.waitForFinalMemoryStatus());
        assertTrue(trade.similarTradeCount() > 0, "Expected at least one similar trade");
        long similarTradeId = trade.openFirstSimilarTradeAndReturnId();
        assertNotEquals(tradeId, similarTradeId, "Similar trade must not be the current trade");
    }
}
