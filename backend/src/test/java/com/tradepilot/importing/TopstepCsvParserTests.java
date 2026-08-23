package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopstepCsvParserTests {
    private final TradingSessionClassifier sessionClassifier = new TradingSessionClassifier();
    private final TopstepCsvParser parser = new TopstepCsvParser(new FuturesPointValueService(), sessionClassifier, new com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer());

    @Test
    void reconstructsLongLosingTradeFromOpeningBidAndClosingAsk() {
        ParsedImport parsed = parser.parse(csv(
                row("1", "APEX 50K", "MESU6", "Filled", "Market", "1", "Bid", "06/15/2026 18:56:13 -05:00", "06/16/2026 00:00:00 -05:00", "5000.00", "Opening", "Trader"),
                row("2", "APEX 50K", "MESU6", "Filled", "Market", "1", "Ask", "06/15/2026 19:02:13 -05:00", "06/16/2026 00:00:00 -05:00", "4984.75", "Closing", "Trader"),
                row("3", "APEX 50K", "MESU6", "Cancelled", "Stop", "1", "Ask", "", "06/16/2026 00:00:00 -05:00", "", "Undetermined", "StopLoss")
        ), 6L, "orders_export.csv");

        assertThat(parsed.totalRows()).isEqualTo(3);
        assertThat(parsed.filledRows()).isEqualTo(2);
        assertThat(parsed.cancelledRowsIgnored()).isEqualTo(1);
        assertThat(parsed.trades()).hasSize(1);

        TradeImportRequest trade = parsed.trades().get(0);
        assertThat(trade.symbol()).isEqualTo("MES");
        assertThat(trade.originalBrokerSymbol()).isEqualTo("MESU6");
        assertThat(trade.contractSymbol()).isEqualTo("MESU6");
        assertThat(trade.contractSymbolSource()).isEqualTo("IMPORTED");
        assertThat(trade.side()).isEqualTo("LONG");
        assertThat(trade.session()).isEqualTo(com.tradepilot.enums.SessionType.ASIA);
        assertThat(trade.tradeDate()).hasToString("2026-06-16");
        assertThat(trade.entryTime()).hasToString("2026-06-15T18:56:13");
        assertThat(trade.exitTime()).hasToString("2026-06-15T19:02:13");
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("-76.25"));
        assertThat(trade.externalTradeKey()).startsWith("TOPSTEP|6|APEX 50K|MESU6|LONG|");
    }

    @Test
    void reconstructsShortWinningTradeFromOpeningAskAndClosingBid() {
        ParsedImport parsed = parser.parse(csv(
                row("10", "APEX 50K", "MNQU6", "Filled", "Limit", "2", "Ask", "07/15/2026 09:35:15 -05:00", "07/15/2026 00:00:00 -05:00", "21000.00", "Opening", "Trader"),
                row("11", "APEX 50K", "MNQU6", "Filled", "Limit", "2", "Bid", "07/15/2026 09:42:15 -05:00", "07/15/2026 00:00:00 -05:00", "20990.00", "Closing", "TakeProfit")
        ), 6L, "orders_export.csv");

        TradeImportRequest trade = parsed.trades().get(0);
        assertThat(trade.side()).isEqualTo("SHORT");
        assertThat(trade.session()).isEqualTo(com.tradepilot.enums.SessionType.NY_AM);
        assertThat(trade.quantity()).isEqualTo(2);
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void classifiesFromFilledAtInstantNotTradeDayMidnight() {
        ParsedImport parsed = parser.parse(csv(
                row("12", "APEX 50K", "MESU6", "Filled", "Market", "1", "Bid", "06/15/2026 18:56:13 -05:00", "06/16/2026 00:00:00 -05:00", "5000.00", "Opening", "Trader"),
                row("13", "APEX 50K", "MESU6", "Filled", "Market", "1", "Ask", "06/15/2026 19:01:13 -05:00", "06/16/2026 00:00:00 -05:00", "5001.00", "Closing", "Trader")
        ), 6L, "orders_export.csv");

        TradeImportRequest trade = parsed.trades().get(0);
        assertThat(trade.tradeDate()).hasToString("2026-06-16");
        assertThat(trade.entryTime()).hasToString("2026-06-15T18:56:13");
        assertThat(trade.session()).isEqualTo(com.tradepilot.enums.SessionType.ASIA);
    }

    @Test
    void combinesPartialClosingFillsUntilPositionIsFlat() {
        ParsedImport parsed = parser.parse(csv(
                row("20", "APEX 50K", "MESM6", "Filled", "Market", "2", "Bid", "06/18/2026 09:30:00 -05:00", "06/18/2026 00:00:00 -05:00", "5000.00", "Opening", "Trader"),
                row("21", "APEX 50K", "MESM6", "Filled", "Limit", "1", "Ask", "06/18/2026 09:35:00 -05:00", "06/18/2026 00:00:00 -05:00", "5002.00", "Closing", "TakeProfit"),
                row("22", "APEX 50K", "MESM6", "Filled", "Limit", "1", "Ask", "06/18/2026 09:40:00 -05:00", "06/18/2026 00:00:00 -05:00", "5004.00", "Closing", "TakeProfit")
        ), 6L, "orders_export.csv");

        TradeImportRequest trade = parsed.trades().get(0);
        assertThat(trade.quantity()).isEqualTo(2);
        assertThat(trade.exitPrice()).isEqualByComparingTo(new BigDecimal("5003.000000"));
        assertThat(trade.pnlAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(parsed.sourceExecutionIds().get(0)).containsExactly("20", "21", "22");
    }

    @Test
    void rejectsMultipleSourceAccountsInOneTopstepFile() {
        String csv = csv(
                row("1", "APEX 50K", "MESU6", "Filled", "Market", "1", "Bid", "06/15/2026 18:56:13 -05:00", "06/16/2026 00:00:00 -05:00", "5000.00", "Opening", "Trader"),
                row("2", "TOPSTEP 50K", "MESU6", "Filled", "Market", "1", "Ask", "06/15/2026 19:02:13 -05:00", "06/16/2026 00:00:00 -05:00", "4984.75", "Closing", "Trader")
        );

        assertThatThrownBy(() -> parser.parse(csv, 6L, "orders_export.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one source AccountName");
    }

    @Test
    void reportsIncompleteOpenPositionWithoutPersistableTrade() {
        ParsedImport parsed = parser.parse(csv(
                row("1", "APEX 50K", "MESU6", "Filled", "Market", "1", "Bid", "06/15/2026 18:56:13 -05:00", "06/16/2026 00:00:00 -05:00", "5000.00", "Opening", "Trader")
        ), 6L, "orders_export.csv");

        assertThat(parsed.trades()).isEmpty();
        assertThat(parsed.incompletePositions()).isEqualTo(1);
        assertThat(parsed.warnings()).anyMatch(warning -> warning.contains("never returned flat"));
    }

    private String csv(String... rows) {
        return "Id,AccountName,ContractName,Status,Type,Size,Side,FilledAt,TradeDay,ExecutePrice,PositionDisposition,CreationDisposition\n"
                + String.join("\n", rows);
    }

    private String row(String id, String accountName, String contract, String status, String type, String size, String side, String filledAt, String tradeDay, String executePrice, String positionDisposition, String creationDisposition) {
        return String.join(",", id, accountName, contract, status, type, size, side, filledAt, tradeDay, executePrice, positionDisposition, creationDisposition);
    }
}
