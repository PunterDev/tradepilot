package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.LoginPage;
import com.tradepilot.e2e.pages.TradeDetailPage;

@Tag("e2e")
class TradeDetailTest extends BaseTest {
    @Test
    void knownSeededTradeCanOpenTradeDetail() {
        DashboardPage dashboard = new LoginPage(driver, wait)
            .open(baseUrl)
            .login(testData.proUser().email(), testData.proUser().password());

        assertTrue(dashboard.isDisplayed());
        TradeDetailPage trade = dashboard.openKnownTrade(testData.proUser().tradeId());

        assertTrue(trade.symbol().length() > 0, "Trade detail should show a symbol");
        assertTrue(trade.side().length() > 0, "Trade detail should show a direction");
        assertTrue(trade.entry().length() > 0, "Trade detail should show an entry price");
        assertTrue(trade.exit().length() > 0, "Trade detail should show an exit price");
        assertTrue(trade.pnl().length() > 0, "Trade detail should show P&L");
    }
}
