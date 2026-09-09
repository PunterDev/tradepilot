package com.tradepilot.e2e.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tradepilot.e2e.base.BaseTest;
import com.tradepilot.e2e.pages.DashboardPage;
import com.tradepilot.e2e.pages.ImportPage;
import com.tradepilot.e2e.pages.LoginPage;

@Tag("e2e")
class TradeImportTest extends BaseTest {
    @Test
    void topstepCsvShowsAValidPreview() {
        rejectProductionMutation("topstepCsvShowsAValidPreview");
        DashboardPage dashboard = new LoginPage(driver, wait)
            .open(baseUrl)
            .login(testData.proUser().email(), testData.proUser().password());

        assertTrue(dashboard.isDisplayed());
        Path csv = Path.of("src/test/resources/test-data/topstep-orders.csv").toAbsolutePath();
        ImportPage preview = dashboard.importPanel()
                .selectAccount(testData.proUser().primaryAccountId())
                .upload(csv.toString());

        assertTrue(preview.broker().contains("TOPSTEP"), "Detected format should be TOPSTEP");
        assertEquals(1, preview.tradeCount(), "Fixture should preview one completed trade");
        assertEquals(1, preview.cancelledIgnored(), "Fixture includes one cancelled order that should be ignored");
        assertEquals(0, preview.duplicateCount(), "Fresh preview fixture should not report duplicates");
    }
}
