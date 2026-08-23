package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradovateCsvParserTests {
    private final TradingSessionClassifier sessionClassifier = new TradingSessionClassifier();
    private final TradovateCsvParser parser = new TradovateCsvParser(
            sessionClassifier,
            new com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer(),
            new FuturesPointValueService()
    );

    @Test
    void parsesTradovateTradeWithExistingExternalKeyShape() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,1,5000.00,5010.00,$50.00,6/16/2026 9:30:00 AM,6/16/2026 9:45:00 AM,15m
                """;

        ParsedImport parsed = parser.parse(csv, 6L, "tradovate.csv");

        assertThat(parsed.format().name()).isEqualTo("TRADOVATE");
        assertThat(parsed.trades()).hasSize(1);
        TradeImportRequest trade = parsed.trades().get(0);
        assertThat(trade.symbol()).isEqualTo("MES");
        assertThat(trade.originalBrokerSymbol()).isEqualTo("MESU6");
        assertThat(trade.contractSymbol()).isEqualTo("MESU6");
        assertThat(trade.contractSymbolSource()).isEqualTo("IMPORTED");
        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.session()).isEqualTo(com.tradepilot.enums.SessionType.NY_AM);
        assertThat(trade.tradeDate()).hasToString("2026-06-16");
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(trade.externalTradeKey()).isEqualTo("6|MESU6|6/16/2026 9:30:00 AM|6/16/2026 9:45:00 AM|1|5000|5010|50");
    }

    @Test
    void classifiesUnmappedTradovateTimesAsUnknownInsteadOfNyAm() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,1,5000.00,4999.00,-$5.00,6/16/2026 5:15:00 PM,6/16/2026 5:20:00 PM,5m
                """;

        ParsedImport parsed = parser.parse(csv, 6L, "tradovate.csv");

        assertThat(parsed.trades().get(0).session()).isEqualTo(com.tradepilot.enums.SessionType.UNKNOWN);
    }

    @Test
    void treatsOffsetlessTradovateTimestampsAsMarketLocalTime() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,1,5000.00,5001.00,$5.00,3/9/2026 9:30:00 AM,3/9/2026 9:35:00 AM,5m
                """;

        ParsedImport parsed = parser.parse(csv, 6L, "tradovate.csv");

        assertThat(parsed.trades().get(0).session()).isEqualTo(com.tradepilot.enums.SessionType.NY_AM);
    }

    @Test
    void rejectsMissingTradovateTimestampWithRowWarning() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,1,5000.00,5001.00,$5.00,,3/9/2026 9:35:00 AM,5m
                """;

        assertThatThrownBy(() -> parser.parse(csv, 6L, "tradovate.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No valid Tradovate trades");
    }

    @Test
    void rejectsMalformedTradovateTimestampWithRowWarning() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,1,5000.00,5001.00,$5.00,not-a-time,3/9/2026 9:35:00 AM,5m
                """;

        assertThatThrownBy(() -> parser.parse(csv, 6L, "tradovate.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No valid Tradovate trades");
    }

    @Test
    void importsMnqLongWinnerWithCalculatedPnlConsistency() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,2,18820.25,18839.25,$76.00,6/12/2026 10:54:00 AM,6/12/2026 10:57:00 AM,3m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MNQ");
        assertThat(trade.originalBrokerSymbol()).isEqualTo("MNQM6");
        assertThat(trade.contractSymbol()).isEqualTo("MNQM6");
        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("18820.25"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("18839.25"));
        assertThat(trade.quantity()).isEqualTo(2);
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("76.00"));
    }

    @Test
    void importsMnqLongLoserWithCalculatedPnlConsistency() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,2,18839.25,18820.25,-$76.00,6/12/2026 10:54:00 AM,6/12/2026 10:57:00 AM,3m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MNQ");
        assertThat(trade.contractSymbol()).isEqualTo("MNQM6");
        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("18839.25"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("18820.25"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("-76.00"));
    }

    @Test
    void importsMnqShortWinnerWithCalculatedPnlConsistency() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,2,18820.25,18839.25,$76.00,6/12/2026 10:57:00 AM,6/12/2026 10:54:00 AM,3m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MNQ");
        assertThat(trade.contractSymbol()).isEqualTo("MNQM6");
        assertThat(trade.side()).isEqualTo("SHORT");
        assertThat(trade.entryTime()).hasToString("2026-06-12T10:54");
        assertThat(trade.exitTime()).hasToString("2026-06-12T10:57");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("18839.25"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("18820.25"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("76.00"));
    }

    @Test
    void importsMnqShortLoserWithCalculatedPnlConsistency() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,2,18839.25,18820.25,-$76.00,6/12/2026 10:57:00 AM,6/12/2026 10:54:00 AM,3m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MNQ");
        assertThat(trade.contractSymbol()).isEqualTo("MNQM6");
        assertThat(trade.side()).isEqualTo("SHORT");
        assertThat(trade.entryTime()).hasToString("2026-06-12T10:54");
        assertThat(trade.exitTime()).hasToString("2026-06-12T10:57");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("18820.25"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("18839.25"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("-76.00"));
    }

    @Test
    void rejectsContradictoryMnqShortLoserBrokerPnl() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,3,18839.25,18820.25,$90.00,6/12/2026 10:57:00 AM,6/12/2026 10:54:00 AM,3m
                """;

        assertThatThrownBy(() -> parser.parse(csv, 6L, "tradovate.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No valid Tradovate trades");
    }

    @Test
    void recordsWarningForContradictoryTradovatePnlWhenOtherRowsRemainValid() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,3,18839.25,18820.25,$90.00,6/12/2026 10:57:00 AM,6/12/2026 10:54:00 AM,3m
                MESU6,1,5000.00,5001.00,$5.00,6/12/2026 9:30:00 AM,6/12/2026 9:35:00 AM,5m
                """;

        ParsedImport parsed = parser.parse(csv, 6L, "tradovate.csv");

        assertThat(parsed.trades()).hasSize(1);
        assertThat(parsed.invalidRows()).isEqualTo(1);
        assertThat(parsed.warnings()).anySatisfy(warning -> {
            assertThat(warning).contains("Row 2 has inconsistent Tradovate pnl");
            assertThat(warning).contains("calculatedGrossPnl=-114.00");
            assertThat(warning).contains("brokerReportedPnl=90.00");
            assertThat(warning).contains("pnlDelta=204.00");
        });
    }

    @Test
    void importsMesUsingCanonicalPointValue() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MESU6,3,5000.00,5001.00,$15.00,6/12/2026 9:30:00 AM,6/12/2026 9:35:00 AM,5m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MES");
        assertThat(trade.contractSymbol()).isEqualTo("MESU6");
        assertThat(trade.quantity()).isEqualTo(3);
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void importsAggregatedPartialFillRowUsingProvidedWeightedAveragePrices() {
        String csv = """
                Symbol,Qty,Buy Price,Sell Price,PnL,Bought Timestamp,Sold Timestamp,Duration
                MNQM6,3,18839.25,18820.25,-$114.00,6/12/2026 10:57:00 AM,6/12/2026 10:54:00 AM,3m
                """;

        TradeImportRequest trade = parser.parse(csv, 6L, "tradovate.csv").trades().get(0);

        assertThat(trade.symbol()).isEqualTo("MNQ");
        assertThat(trade.originalBrokerSymbol()).isEqualTo("MNQM6");
        assertThat(trade.contractSymbol()).isEqualTo("MNQM6");
        assertThat(trade.side()).isEqualTo("SHORT");
        assertThat(trade.quantity()).isEqualTo(3);
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("18820.25"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("18839.25"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("-114.00"));
    }
}
