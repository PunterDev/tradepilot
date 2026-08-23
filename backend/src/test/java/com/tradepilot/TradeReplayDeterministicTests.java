package com.tradepilot;

import com.tradepilot.entity.Trade;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.tradereplay.service.FuturesContractResolver;
import com.tradepilot.tradereplay.service.FuturesContractCalendar;
import com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer;
import com.tradepilot.tradereplay.service.HistoricalCandle;
import com.tradepilot.tradereplay.service.TradeReplayAnalysis;
import com.tradepilot.tradereplay.service.TradeReplayAnalysisEngine;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeReplayDeterministicTests {
    private final FuturesContractSymbolNormalizer normalizer = new FuturesContractSymbolNormalizer();
    private final FuturesContractCalendar calendar = new FuturesContractCalendar(normalizer);
    private final FuturesContractResolver resolver = new FuturesContractResolver(normalizer, calendar);
    private final TradeReplayAnalysisEngine engine = new TradeReplayAnalysisEngine();

    @Test
    void resolvesSupportedContractsAndRejectsContinuousSymbols() {
        Trade mes = trade("MESU6", "LONG", "4500.00", "4502.00");
        assertThat(resolver.resolve(mes).providerSymbol()).isEqualTo("MESU6");

        Trade mnqLongYear = trade("mnq-u-2026", "LONG", "18000.00", "18020.00");
        assertThat(resolver.resolve(mnqLongYear).providerSymbol()).isEqualTo("MNQU6");

        Trade continuous = trade("NQ", "LONG", "18000.00", "18020.00");
        assertThat(resolver.resolve(continuous).resolved()).isFalse();
        assertThat(resolver.resolve(continuous).failureCode()).isEqualTo("CONTRACT_REQUIRED");
    }

    @Test
    void normalizesBrokerContractFormats() {
        assertThat(normalizer.normalize("/MNQU6", LocalDateTime.of(2026, 9, 1, 9, 30)).normalizedContractSymbol()).isEqualTo("MNQU6");
        assertThat(normalizer.normalize("MNQ U6", LocalDateTime.of(2026, 9, 1, 9, 30)).normalizedContractSymbol()).isEqualTo("MNQU6");
        assertThat(normalizer.normalize("MNQU26", LocalDateTime.of(2026, 9, 1, 9, 30)).normalizedContractSymbol()).isEqualTo("MNQU6");
        assertThat(normalizer.normalize("MNQ 09-26", LocalDateTime.of(2026, 9, 1, 9, 30)).normalizedContractSymbol()).isEqualTo("MNQU6");
    }

    @Test
    void dateValidatedGeneratedContractsFallBackAfterExpiration() {
        Trade trade = trade("MES", "LONG", "5300.00", "5303.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 22, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 22, 14, 45));
        trade.setContractSymbol("MESM6");
        trade.setContractSymbolSource(ContractSymbolSource.NORMALIZED.name());

        var resolution = resolver.resolve(trade);

        assertThat(resolution.providerSymbol()).isEqualTo("MESU6");
        assertThat(resolution.requestedContract()).isEqualTo("MESM6");
        assertThat(resolution.expectedContract()).isEqualTo("MESU6");
        assertThat(resolution.resolutionReason()).isEqualTo("DATE_VALIDATED_FALLBACK");
        assertThat(resolution.storedContractValidForDate()).isFalse();
    }

    @Test
    void inferredMnqContractFallsBackAfterJuneExpiration() {
        Trade trade = trade("MNQ", "LONG", "18820.25", "18830.25");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 22, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 22, 14, 45));
        trade.setContractSymbol("MNQM6");
        trade.setContractSymbolSource(ContractSymbolSource.ADMIN_BACKFILL.name());

        var resolution = resolver.resolve(trade);

        assertThat(resolution.providerSymbol()).isEqualTo("MNQU6");
        assertThat(resolution.resolutionReason()).isEqualTo("DATE_VALIDATED_FALLBACK");
    }

    @Test
    void userConfirmedContractRemainsAuthoritativeEvenWhenNotExpectedActive() {
        Trade trade = trade("MES", "LONG", "5300.00", "5303.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 22, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 22, 14, 45));
        trade.setOriginalBrokerSymbol("MESM6");
        trade.setContractSymbol("MESM6");
        trade.setContractSymbolSource(ContractSymbolSource.USER_CONFIRMED.name());

        var resolution = resolver.resolve(trade);

        assertThat(resolution.providerSymbol()).isEqualTo("MESM6");
        assertThat(resolution.expectedContract()).isEqualTo("MESU6");
        assertThat(resolution.resolutionReason()).isEqualTo("TRUST_EXACT_CONTRACT");
    }

    @Test
    void importedBrokerContractRemainsAuthoritativeEvenWhenNotExpectedActive() {
        Trade trade = trade("MES", "LONG", "5300.00", "5303.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 22, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 22, 14, 45));
        trade.setOriginalBrokerSymbol("MESM6");
        trade.setContractSymbol("MESM6");
        trade.setContractSymbolSource(ContractSymbolSource.IMPORTED.name());

        var resolution = resolver.resolve(trade);

        assertThat(resolution.providerSymbol()).isEqualTo("MESM6");
        assertThat(resolution.expectedContract()).isEqualTo("MESU6");
        assertThat(resolution.resolutionReason()).isEqualTo("TRUST_EXACT_CONTRACT");
    }

    @Test
    void demoTradeCanFallbackEvenWhenPersistedSourceLooksUserConfirmed() {
        Trade trade = trade("MES", "LONG", "5300.00", "5303.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 22, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 22, 14, 45));
        trade.setExternalTradeKey("DEV-DEMO-1-42");
        trade.setContractSymbol("MESM6");
        trade.setContractSymbolSource(ContractSymbolSource.USER_CONFIRMED.name());

        var resolution = resolver.resolve(trade);

        assertThat(resolution.providerSymbol()).isEqualTo("MESU6");
        assertThat(resolution.resolutionReason()).isEqualTo("DATE_VALIDATED_FALLBACK");
    }

    @Test
    void calculatesLongMfeMaeAndCapture() {
        Trade trade = trade("ESU6", "LONG", "5000.00", "5004.00");
        trade.setStopPrice(new BigDecimal("4998.00"));
        trade.setQuantity(1);

        TradeReplayAnalysis analysis = engine.analyze(trade, candles(), "ES");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("3.00"));
        assertThat(analysis.metrics()).containsEntry("mfeDollars", new BigDecimal("500.00"));
        assertThat(analysis.metrics()).containsEntry("maeDollars", new BigDecimal("150.00"));
        assertThat(analysis.metrics()).containsEntry("realizedR", new BigDecimal("2.00"));
        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .contains(com.tradepilot.tradereplay.entity.TradeReplayInsightType.MFE,
                        com.tradepilot.tradereplay.entity.TradeReplayInsightType.MAE);
    }

    @Test
    void reversesFavorableAndAdverseForShortTrades() {
        Trade trade = trade("MNQU6", "SHORT", "18000.00", "17992.00");
        trade.setStopPrice(new BigDecimal("18010.00"));
        trade.setQuantity(2);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "18001", "18005", "17995", "17996"),
                candle("2026-09-01T19:31:00Z", "17996", "18003", "17980", "17988")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("20.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("5.00"));
        assertThat(analysis.metrics()).containsEntry("mfeDollars", new BigDecimal("80.00"));
        assertThat(analysis.metrics()).containsEntry("maeDollars", new BigDecimal("20.00"));
    }

    @Test
    void calculatesLongWinnerExcursionsInsideTradeWindow() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "110.00");
        trade.setQuantity(2);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "108", "97", "107"),
                candle("2026-09-01T19:35:00Z", "107", "115", "104", "110")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("15.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("3.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("mfeDollars", new BigDecimal("60.00"));
    }

    @Test
    void calculatesLongLoserExcursionsAsNonNegativeMagnitudes() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "95.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "103", "96", "98"),
                candle("2026-09-01T19:35:00Z", "98", "100", "90", "95")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("3.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("-5.00"));
    }

    @Test
    void calculatesShortWinnerExcursionsAsNonNegativeMagnitudes() {
        Trade trade = trade("MNQM6", "SHORT", "100.00", "90.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "104", "95", "96"),
                candle("2026-09-01T19:35:00Z", "96", "99", "85", "90")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("15.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("4.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("10.00"));
    }

    @Test
    void calculatesShortLoserExcursionsAsNonNegativeMagnitudes() {
        Trade trade = trade("MNQM6", "SHORT", "100.00", "105.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "104", "97", "102"),
                candle("2026-09-01T19:35:00Z", "102", "110", "99", "105")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("3.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("-5.00"));
    }

    @Test
    void excludesPreTradeAndPostExitCandlesFromTradeExcursions() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "105.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:29:00Z", "100", "150", "99", "100"),
                candle("2026-09-01T19:30:00Z", "100", "108", "98", "104"),
                candle("2026-09-01T19:40:00Z", "104", "110", "101", "105"),
                candle("2026-09-01T19:41:00Z", "105", "140", "104", "130")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("2.00"));
        assertThat(analysis.metrics()).containsEntry("additionalFavorableAfterExitPoints", new BigDecimal("35.00"));
    }

    @Test
    void keepsPointsIndependentFromQuantityAndAppliesQuantityToDollars() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "106.00");
        trade.setQuantity(3);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "110", "98", "106")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("mfeDollars", new BigDecimal("60.00"));
    }

    @Test
    void returnsUnavailableExcursionsForUnknownSideOrMissingEntryPrice() {
        Trade unknownSide = trade("MNQM6", "SIDEWAYS", "100.00", "106.00");
        TradeReplayAnalysis unknownAnalysis = engine.analyze(unknownSide, List.of(
                candle("2026-09-01T19:30:00Z", "100", "110", "98", "106")
        ), "MNQ");

        assertThat(unknownAnalysis.metrics()).containsEntry("direction", "UNKNOWN");
        assertThat(unknownAnalysis.metrics()).containsEntry("mfePoints", null);
        assertThat(unknownAnalysis.metrics()).containsEntry("maePoints", null);

        Trade missingEntry = trade("MNQM6", "LONG", "100.00", "106.00");
        missingEntry.setEntryPrice(null);
        TradeReplayAnalysis missingAnalysis = engine.analyze(missingEntry, List.of(
                candle("2026-09-01T19:30:00Z", "100", "110", "98", "106")
        ), "MNQ");

        assertThat(missingAnalysis.metrics()).containsEntry("mfePoints", null);
        assertThat(missingAnalysis.metrics()).containsEntry("maePoints", null);
    }

    @Test
    void keepsRealisticMnqReplayValuesInSameUnitsAsTradePrices() {
        Trade trade = trade("MNQM6", "LONG", "21450.00", "21427.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 4));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 4, 13, 57));
        trade.setExitTime(LocalDateTime.of(2026, 6, 4, 14, 10));

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-04T18:57:00Z", "21445", "21452", "21440", "21448"),
                candle("2026-06-04T19:10:00Z", "21448", "21480", "21420", "21427"),
                candle("2026-06-04T20:20:00Z", "21427", "21580", "21420", "21550")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("30.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("30.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("-23.00"));
    }

    @Test
    void analyzesEarlyMorningCentralTradeAgainstCorrectUtcCandleWindow() {
        Trade trade = trade("MNQM6", "SHORT", "29190.00", "29178.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 8));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 8, 5, 25));
        trade.setExitTime(LocalDateTime.of(2026, 6, 8, 5, 28));
        trade.setQuantity(3);
        trade.setRiskAmount(new BigDecimal("90.00"));
        trade.setPnlAmount(new BigDecimal("72.00"));
        trade.setPnlR(new BigDecimal("0.80"));

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-08T10:25:00Z", "29188.50", "29196.25", "29180.50", "29185.00"),
                candle("2026-06-08T10:26:00Z", "29185.75", "29188.75", "29178.75", "29184.75"),
                candle("2026-06-08T10:27:00Z", "29184.75", "29185.75", "29177.00", "29181.00"),
                candle("2026-06-08T10:28:00Z", "29180.00", "29187.00", "29175.75", "29181.75")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("14.25"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("6.25"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("12.00"));
        assertThat(analysis.metrics()).containsEntry("mfeDollars", new BigDecimal("85.50"));
        assertThat(analysis.metrics()).containsEntry("maeDollars", new BigDecimal("37.50"));
        assertThat(analysis.metrics()).containsEntry("excursionCapturePct", new BigDecimal("84.21"));
        assertThat(analysis.insights().get(0).type()).isEqualTo(TradeReplayInsightType.STRONG_EXCURSION_CAPTURE);
        assertThat(analysis.insights().get(0).title()).isEqualTo("Strong excursion capture");
    }

    @Test
    void ranksStrongCaptureAboveStopContactForStrongWinner() {
        Trade trade = trade("MNQM6", "SHORT", "29190.00", "29178.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 8));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 8, 5, 25));
        trade.setExitTime(LocalDateTime.of(2026, 6, 8, 5, 28));
        trade.setQuantity(3);
        trade.setStopPrice(new BigDecimal("29196.00"));

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-08T10:25:00Z", "29188.50", "29196.25", "29180.50", "29185.00"),
                candle("2026-06-08T10:28:00Z", "29180.00", "29187.00", "29175.75", "29181.75")
        ), "MNQ");

        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .startsWith(TradeReplayInsightType.STRONG_EXCURSION_CAPTURE);
        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .contains(TradeReplayInsightType.STOP_LEVEL_TRADED_DURING_POSITION);
    }

    @Test
    void ranksLargeGivebackForWinnerWithHugeOpenProfitGiveback() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "108.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "130", "99", "128"),
                candle("2026-09-01T19:40:00Z", "128", "129", "107", "108")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("30.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("8.00"));
        assertThat(analysis.insights().get(0).type()).isEqualTo(TradeReplayInsightType.LARGE_PROFIT_GIVEBACK);
    }

    @Test
    void ranksWinnerTurnedLoserForLosingTradeWithMeaningfulMfe() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "95.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "115", "99", "112"),
                candle("2026-09-01T19:40:00Z", "112", "113", "94", "95")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("15.00"));
        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("-5.00"));
        assertThat(analysis.insights().get(0).type()).isEqualTo(TradeReplayInsightType.WINNER_TURNED_LOSER);
    }

    @Test
    void ranksLargePostExitContinuationWithoutHindsightLanguage() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "105.00");

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "106", "99", "105"),
                candle("2026-09-01T19:41:00Z", "105", "122", "104", "120")
        ), "MNQ");

        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .contains(TradeReplayInsightType.POST_EXIT_CONTINUATION);
        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::explanation)
                .anySatisfy(text -> assertThat(text).contains("continuation alone does not make the exit wrong"));
    }

    @Test
    void stopTouchedAfterExitDoesNotImplyOpenPositionRisk() {
        Trade trade = trade("MNQM6", "LONG", "100.00", "105.00");
        trade.setStopPrice(new BigDecimal("95.00"));

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-09-01T19:30:00Z", "100", "106", "99", "105"),
                candle("2026-09-01T19:41:00Z", "105", "106", "95", "96")
        ), "MNQ");

        assertThat(analysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .contains(TradeReplayInsightType.STOP_LEVEL_TRADED_AFTER_EXIT)
                .doesNotContain(TradeReplayInsightType.STOP_LEVEL_TRADED_DURING_POSITION);
    }

    @Test
    void captureHandlesZeroMfeAndLosersWithoutDivideByZero() {
        Trade flat = trade("MNQM6", "LONG", "100.00", "100.00");
        TradeReplayAnalysis flatAnalysis = engine.analyze(flat, List.of(
                candle("2026-09-01T19:30:00Z", "100", "100", "100", "100")
        ), "MNQ");

        assertThat(flatAnalysis.metrics()).containsEntry("excursionCapturePct", null);

        Trade loser = trade("MNQM6", "LONG", "100.00", "95.00");
        TradeReplayAnalysis loserAnalysis = engine.analyze(loser, List.of(
                candle("2026-09-01T19:30:00Z", "100", "103", "94", "95")
        ), "MNQ");

        assertThat(loserAnalysis.metrics()).containsEntry("excursionCapturePct", new BigDecimal("0.00"));
        assertThat(loserAnalysis.insights()).extracting(TradeReplayAnalysis.InsightDraft::type)
                .doesNotContain(TradeReplayInsightType.LOW_EXCURSION_CAPTURE);
    }

    @Test
    void analyzesMesShortLoserFromPlausibleTradeWindowCandles() {
        Trade trade = trade("MESM6", "SHORT", "5315.75", "5340.75");
        trade.setTradeDate(LocalDate.of(2026, 6, 10));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 10, 14, 19));
        trade.setExitTime(LocalDateTime.of(2026, 6, 10, 14, 19, 45));
        trade.setQuantity(3);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-10T19:19:00Z", "5315.75", "5341.25", "5314.25", "5340.75")
        ), "MES");

        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("-25.00"));
        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("1.50"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("25.50"));
        assertThat(analysis.metrics()).containsEntry("maeDollars", new BigDecimal("382.50"));
    }

    @Test
    void demoReplayFixturePricesStayInSameRegionAsCandles() {
        Trade trade = trade("MNQM6", "SHORT", "30580.00", "30560.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 4));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 4, 14, 12));
        trade.setExitTime(LocalDateTime.of(2026, 6, 4, 14, 24));
        trade.setQuantity(3);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-04T19:12:00Z", "30582.00", "30590.00", "30570.00", "30576.00"),
                candle("2026-06-04T19:20:00Z", "30576.00", "30582.00", "30550.00", "30560.00")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("20.00"));
        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("30.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("10.00"));
        assertThat(analysis.metrics()).containsEntry("realizedDollars", new BigDecimal("120.00"));
    }

    @Test
    void correctedMesu6DemoFixturePricesStayInSameRegionAsCandles() {
        Trade trade = trade("MESU6", "SHORT", "7492.00", "7480.00");
        trade.setTradeDate(LocalDate.of(2026, 7, 8));
        trade.setEntryTime(LocalDateTime.of(2026, 7, 8, 5, 45));
        trade.setExitTime(LocalDateTime.of(2026, 7, 8, 5, 57));
        trade.setQuantity(3);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-07-08T10:45:00Z", "7491.50", "7497.00", "7487.00", "7488.00"),
                candle("2026-07-08T10:52:00Z", "7488.00", "7490.00", "7475.00", "7480.00")
        ), "MES");

        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("12.00"));
        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("17.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("5.00"));
        assertThat(analysis.metrics()).containsEntry("realizedDollars", new BigDecimal("180.00"));
    }

    @Test
    void correctedMnqu6DemoFixturePricesStayInSameRegionAsCandles() {
        Trade trade = trade("MNQU6", "SHORT", "29820.00", "29805.00");
        trade.setTradeDate(LocalDate.of(2026, 6, 24));
        trade.setEntryTime(LocalDateTime.of(2026, 6, 24, 14, 33));
        trade.setExitTime(LocalDateTime.of(2026, 6, 24, 14, 44));
        trade.setQuantity(3);

        TradeReplayAnalysis analysis = engine.analyze(trade, List.of(
                candle("2026-06-24T19:33:00Z", "29818.00", "29825.00", "29810.00", "29812.00"),
                candle("2026-06-24T19:40:00Z", "29812.00", "29816.00", "29798.00", "29805.00")
        ), "MNQ");

        assertThat(analysis.metrics()).containsEntry("realizedPoints", new BigDecimal("15.00"));
        assertThat(analysis.metrics()).containsEntry("mfePoints", new BigDecimal("22.00"));
        assertThat(analysis.metrics()).containsEntry("maePoints", new BigDecimal("5.00"));
        assertThat(analysis.metrics()).containsEntry("realizedDollars", new BigDecimal("90.00"));
    }

    private Trade trade(String symbol, String side, String entry, String exit) {
        return Trade.builder()
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 9, 1))
                .entryTime(LocalDateTime.of(2026, 9, 1, 14, 30))
                .exitTime(LocalDateTime.of(2026, 9, 1, 14, 40))
                .side(side)
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal(entry))
                .exitPrice(new BigDecimal(exit))
                .quantity(1)
                .outcome(OutcomeType.WIN)
                .build();
    }

    private List<HistoricalCandle> candles() {
        return List.of(
                candle("2026-09-01T19:30:00Z", "5000", "5004", "4997", "5003"),
                candle("2026-09-01T19:31:00Z", "5003", "5010", "5002", "5008"),
                candle("2026-09-01T19:41:00Z", "5004", "5008", "5001", "5006")
        );
    }

    private HistoricalCandle candle(String timestamp, String open, String high, String low, String close) {
        return new HistoricalCandle(Instant.parse(timestamp), new BigDecimal(open), new BigDecimal(high), new BigDecimal(low), new BigDecimal(close), BigDecimal.TEN);
    }
}
