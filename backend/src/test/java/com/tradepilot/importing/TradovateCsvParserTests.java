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

    @Test
    void recognizesTradovatePerformanceSchemaWithoutOptionalMetadataColumns() {
        CsvTable table = CsvTable.parse("""
                symbol,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp
                MESU6,411399566963,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23
                """);

        assertThat(parser.supports(table)).isTrue();
    }

    @Test
    void doesNotClassifyRandomCsvAsTradovate() {
        CsvTable table = CsvTable.parse("""
                symbol,qty,pnl,openedAt,closedAt
                MESU6,1,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23
                """);

        assertThat(parser.supports(table)).isFalse();
    }

    @Test
    void mapsTradovatePerformanceLongRowsFromBuyThenSellTimestamps() {
        TradeImportRequest trade = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399566963,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23,2h 11min 14sec
                """, 6L, "Performance.csv").trades().get(0);

        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.entryTime()).hasToString("2026-08-03T09:15:09");
        assertThat(trade.exitTime()).hasToString("2026-08-03T11:26:23");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("7590.75"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("7615.00"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("121.25"));
    }

    @Test
    void classifiesTradovatePerformanceTimestampsAsChicagoBrokerLocalTime() {
        TradeImportRequest trade = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MNQU6,-2,0,0.25,511399566963,511399567030,1,19000.00,19005.00,$10.00,09/03/2026 09:16:00,09/03/2026 09:18:00,2min
                """, 6L, "Performance.csv").trades().get(0);

        assertThat(trade.entryTime()).hasToString("2026-09-03T09:16");
        assertThat(trade.session()).isEqualTo(com.tradepilot.enums.SessionType.NY_AM);
    }

    @Test
    void mapsTradovatePerformanceShortRowsFromSellThenBuyTimestamps() {
        TradeImportRequest trade = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399567095,411399567079,1,7771.00,7785.50,$72.50,08/05/2026 10:18:07,08/05/2026 10:06:28,11min 39sec
                """, 6L, "Performance.csv").trades().get(0);

        assertThat(trade.side()).isEqualTo("SHORT");
        assertThat(trade.entryTime()).hasToString("2026-08-05T10:06:28");
        assertThat(trade.exitTime()).hasToString("2026-08-05T10:18:07");
        assertThat(trade.entryPrice()).isEqualByComparingTo(new BigDecimal("7785.50"));
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("7771.00"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("72.50"));
    }

    @Test
    void parsesTradovatePerformanceParenthesesPnlAsNegativeMoney() {
        TradeImportRequest trade = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399567046,411399567066,1,7816.25,7800.75,$(77.50),08/05/2026 09:14:29,08/05/2026 09:23:21,8min 52sec
                """, 6L, "Performance.csv").trades().get(0);

        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("-77.50"));
        assertThat(trade.outcome()).isEqualTo(com.tradepilot.enums.OutcomeType.LOSS);
    }

    @Test
    void preservesTradovatePerformanceContractMetadataAndRootSymbol() {
        ParsedImport parsed = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399566963,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23,2h 11min 14sec
                MNQU6,-2,0,0.25,511399566963,511399567030,1,19000.00,19005.00,$10.00,08/06/2026 09:15:09,08/06/2026 09:26:23,11min 14sec
                """, 6L, "Performance.csv");

        TradeImportRequest mes = parsed.trades().get(0);
        assertThat(mes.symbol()).isEqualTo("MES");
        assertThat(mes.originalBrokerSymbol()).isEqualTo("MESU6");
        assertThat(mes.contractSymbol()).isEqualTo("MESU6");
        assertThat(mes.contractSymbolSource()).isEqualTo("IMPORTED");

        TradeImportRequest mnq = parsed.trades().get(1);
        assertThat(mnq.symbol()).isEqualTo("MNQ");
        assertThat(mnq.originalBrokerSymbol()).isEqualTo("MNQU6");
        assertThat(mnq.contractSymbol()).isEqualTo("MNQU6");
        assertThat(mnq.contractSymbolSource()).isEqualTo("IMPORTED");
    }

    @Test
    void keepsTradovatePerformanceRowsWithSharedFillIdsSeparateByCompositeIdentity() {
        ParsedImport parsed = parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,shared-buy,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23,2h 11min 14sec
                MESU6,-2,0,0.25,shared-buy,411399567031,1,7591.00,7616.00,$125.00,08/03/2026 09:15:10,08/03/2026 11:26:24,2h 11min 14sec
                """, 6L, "Performance.csv");

        assertThat(parsed.trades()).hasSize(2);
        assertThat(parsed.trades().get(0).externalTradeKey()).startsWith("TRADOVATE_PERFORMANCE|6|MESU6|shared-buy|411399567030|");
        assertThat(parsed.trades().get(1).externalTradeKey()).startsWith("TRADOVATE_PERFORMANCE|6|MESU6|shared-buy|411399567031|");
        assertThat(parsed.trades().get(0).externalTradeKey()).isNotEqualTo(parsed.trades().get(1).externalTradeKey());
    }

    @Test
    void rejectsTradovatePerformanceEqualTimestampsRatherThanGuessingDirection() {
        assertThatThrownBy(() -> parser.parse("""
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399566963,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 09:15:09,0sec
                """, 6L, "Performance.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No valid Tradovate trades");
    }

    @Test
    void parsesFullTradovatePerformanceFixture() {
        ParsedImport parsed = parser.parse(tradovatePerformanceFixture(), 6L, "Performance.csv");

        assertThat(parsed.trades()).hasSize(14);
        assertThat(parsed.filledRows()).isEqualTo(14);
        assertThat(parsed.invalidRows()).isZero();
        assertThat(parsed.trades()).filteredOn(trade -> "LONG".equals(trade.side())).hasSize(7);
        assertThat(parsed.trades()).filteredOn(trade -> "SHORT".equals(trade.side())).hasSize(7);
        assertThat(parsed.trades()).filteredOn(trade -> trade.pnlAmount().compareTo(BigDecimal.ZERO) > 0).hasSize(8);
        assertThat(parsed.trades()).filteredOn(trade -> trade.pnlAmount().compareTo(BigDecimal.ZERO) < 0).hasSize(6);
        BigDecimal aggregatePnl = parsed.trades().stream()
                .map(TradeImportRequest::pnlAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(aggregatePnl).isEqualByComparingTo(new BigDecimal("126.75"));
    }

    private String tradovatePerformanceFixture() {
        return """
                symbol,_priceFormat,_priceFormatType,_tickSize,buyFillId,sellFillId,qty,buyPrice,sellPrice,pnl,boughtTimestamp,soldTimestamp,duration
                MESU6,-2,0,0.25,411399566963,411399567030,1,7590.75,7615.00,$121.25,08/03/2026 09:15:09,08/03/2026 11:26:23,2h 11min 14sec
                MESU6,-2,0,0.25,411399567046,411399567066,1,7816.25,7800.75,$(77.50),08/05/2026 09:14:29,08/05/2026 09:23:21,8min 52sec
                MESU6,-2,0,0.25,411399567095,411399567079,1,7771.00,7785.50,$72.50,08/05/2026 10:18:07,08/05/2026 10:06:28,11min 39sec
                MNQU6,-2,0,0.25,511399567001,511399567002,1,19000.00,19005.00,$10.00,08/06/2026 09:00:00,08/06/2026 09:10:00,10min
                MNQU6,-2,0,0.25,511399567003,511399567004,1,19010.00,19013.00,$6.00,08/06/2026 09:20:00,08/06/2026 09:25:00,5min
                MNQU6,-2,0,0.25,511399567005,511399567006,1,19020.00,19022.50,$5.00,08/06/2026 09:30:00,08/06/2026 09:35:00,5min
                MNQU6,-2,0,0.25,511399567007,511399567008,1,19033.50,19030.00,$(7.00),08/06/2026 09:40:00,08/06/2026 09:45:00,5min
                MNQU6,-2,0,0.25,511399567009,511399567010,1,19043.00,19040.00,$(6.00),08/06/2026 09:50:00,08/06/2026 09:55:00,5min
                MNQU6,-2,0,0.25,511399567011,511399567012,1,19056.00,19060.00,$8.00,08/06/2026 10:10:00,08/06/2026 10:00:00,10min
                MNQU6,-2,0,0.25,511399567013,511399567014,1,19064.00,19066.00,$4.00,08/06/2026 10:25:00,08/06/2026 10:20:00,5min
                MNQU6,-2,0,0.25,511399567015,511399567016,1,19071.75,19073.50,$3.50,08/06/2026 10:35:00,08/06/2026 10:30:00,5min
                MNQU6,-2,0,0.25,511399567017,511399567018,1,19082.50,19080.00,$(5.00),08/06/2026 10:40:00,08/06/2026 10:36:00,4min
                MNQU6,-2,0,0.25,511399567019,511399567020,1,19092.00,19090.00,$(4.00),08/06/2026 10:50:00,08/06/2026 10:45:00,5min
                MNQU6,-2,0,0.25,511399567021,511399567022,1,19102.00,19100.00,$(4.00),08/06/2026 11:00:00,08/06/2026 10:55:00,5min
                """;
    }
}
