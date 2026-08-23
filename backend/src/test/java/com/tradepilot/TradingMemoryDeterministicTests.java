package com.tradepilot;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.RecurringPatternResponse;
import com.tradepilot.ai.memory.dto.SimilarTradeResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryMetricsResponse;
import com.tradepilot.ai.memory.service.RecurringPatternDetector;
import com.tradepilot.ai.memory.service.TradeMemoryAnalyticsService;
import com.tradepilot.ai.memory.service.TradeMemoryBuilder;
import com.tradepilot.ai.memory.service.TradeMemoryDocument;
import com.tradepilot.ai.memory.service.TradeMemoryNormalizer;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.Trade;
import com.tradepilot.enums.MistakeTag;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradingMemoryDeterministicTests {
    private final TradeMemoryNormalizer normalizer = new TradeMemoryNormalizer();
    private final TradeMemoryBuilder builder = new TradeMemoryBuilder(normalizer);

    @Test
    void memoryDocumentUsesStableOrderingAndHash() {
        Trade trade = Trade.builder()
                .id(10L)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 7, 31))
                .entryTime(LocalDateTime.of(2026, 7, 31, 9, 42))
                .exitTime(LocalDateTime.of(2026, 7, 31, 9, 56))
                .side("LONG")
                .setup(SetupType.IFVG)
                .session(SessionType.NY_AM)
                .pnlAmount(new BigDecimal("120.00"))
                .pnlR(new BigDecimal("0.60"))
                .outcome(OutcomeType.WIN)
                .emotion("fear of giving back profit")
                .mistakeTag(MistakeTag.EARLY_EXIT)
                .followedPlan(false)
                .notes("Price paused and I assumed buyers were weakening.")
                .psychologyNotes("Exited from discomfort.")
                .lessonLearned("Use structural invalidation.")
                .build();
        AiTradeReview review = AiTradeReview.builder()
                .summary("Entry aligned with bias, but exit was driven by discomfort.")
                .mistakesJson("[\"cut winner early\"]")
                .tagsJson("[\"early exit\"]")
                .build();

        TradeMemoryDocument first = builder.build(trade, review);
        TradeMemoryDocument second = builder.build(trade, review);

        assertThat(first.content()).startsWith("Instrument: NQ\nDirection: LONG\nSetup: IFVG");
        assertThat(first.content()).contains("Result: +0.60R");
        assertThat(first.content()).contains("AI review summary: Entry aligned with bias, but exit was driven by discomfort.");
        assertThat(first.content()).doesNotContain("Trade ID");
        assertThat(first.contentHash()).hasSize(64).isEqualTo(second.contentHash());
        assertThat(first.memoryVersion()).isEqualTo(1);
    }

    @Test
    void memoryDocumentOmitsMissingFieldsCleanly() {
        Trade trade = Trade.builder()
                .id(11L)
                .symbol("ES")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 7, 30))
                .side("SHORT")
                .session(SessionType.UNKNOWN)
                .pnlR(new BigDecimal("-0.38"))
                .outcome(OutcomeType.LOSS)
                .build();

        TradeMemoryDocument document = builder.build(trade, null);

        assertThat(document.content()).contains("Instrument: ES");
        assertThat(document.content()).contains("Direction: SHORT");
        assertThat(document.content()).contains("Result: -0.38R");
        assertThat(document.content()).doesNotContain("Trader notes:");
        assertThat(document.content()).doesNotContain("AI review summary:");
    }

    @Test
    void normalizationMapsCommonMistakesAndEmotions() {
        assertThat(normalizer.normalizeMistake("closed too soon")).isEqualTo("CUT_WINNER_EARLY");
        assertThat(normalizer.normalizeMistake("fomo entry")).isEqualTo("FOMO_ENTRY");
        assertThat(normalizer.normalizeEmotion("Fear of giving back profit")).isEqualTo("FEAR");
        assertThat(normalizer.normalizeEmotion("calm")).isEqualTo("CALM");
    }

    @Test
    void analyticsCalculatesAverageMedianAndExitEfficiencyFromProvidedMetrics() {
        TradeMemoryAnalyticsService analytics = new TradeMemoryAnalyticsService();
        List<SimilarTradeResponse> trades = List.of(
                similar(1L, "CUT_WINNER_EARLY", "FEAR", "0.50", "0.20", "2.00"),
                similar(2L, "CUT_WINNER_EARLY", "FEAR", "0.70", "0.25", "1.50"),
                similar(3L, "FOMO_ENTRY", "FOMO", "-0.30", "0.40", "0.80")
        );

        TradingMemoryMetricsResponse metrics = analytics.calculate(trades);

        assertThat(metrics.sampleSize()).isEqualTo(3);
        assertThat(metrics.winningTrades()).isEqualTo(2);
        assertThat(metrics.losingTrades()).isEqualTo(1);
        assertThat(metrics.averageResultR()).isEqualByComparingTo("0.3000");
        assertThat(metrics.medianResultR()).isEqualByComparingTo("0.50");
        assertThat(metrics.averageMaeR()).isEqualByComparingTo("0.2833");
        assertThat(metrics.averageMfeR()).isEqualByComparingTo("1.4333");
        assertThat(metrics.averageExitEfficiency()).isEqualByComparingTo("0.3584");
        assertThat(metrics.mostCommonMistake()).isEqualTo("CUT_WINNER_EARLY");
        assertThat(metrics.mostCommonEmotion()).isEqualTo("FEAR");
    }

    @Test
    void recurringPatternRequiresConfiguredEvidenceThreshold() {
        TradingMemoryProperties properties = new TradingMemoryProperties();
        properties.setMinPatternSample(5);
        properties.setMinPatternOccurrences(3);
        RecurringPatternDetector detector = new RecurringPatternDetector(properties, normalizer);

        RecurringPatternResponse none = detector.detect(List.of(
                similar(1L, "CUT_WINNER_EARLY", "FEAR", "0.50", null, "1.60"),
                similar(2L, "CUT_WINNER_EARLY", "FEAR", "0.40", null, "1.40"),
                similar(3L, "FOMO_ENTRY", "FOMO", "-0.30", null, "0.70"),
                similar(4L, "REVENGE_TRADE", "ANGER", "-0.50", null, "0.20"),
                similar(5L, "OVERTRADING", "FRUSTRATION", "-0.20", null, "0.40")
        ));
        assertThat(none.detected()).isFalse();

        RecurringPatternResponse detected = detector.detect(List.of(
                similar(1L, "CUT_WINNER_EARLY", "FEAR", "0.50", null, "1.60"),
                similar(2L, "CUT_WINNER_EARLY", "FEAR", "0.40", null, "1.40"),
                similar(3L, "CUT_WINNER_EARLY", "FEAR", "0.60", null, "1.80"),
                similar(4L, "CUT_WINNER_EARLY", "FEAR", "0.30", null, "1.20"),
                similar(5L, "FOMO_ENTRY", "FOMO", "-0.20", null, "0.40")
        ));

        assertThat(detected.detected()).isTrue();
        assertThat(detected.patternType()).isEqualTo("CUT_WINNER_EARLY");
        assertThat(detected.occurrences()).isEqualTo(4);
        assertThat(detected.occurrenceRate()).isEqualByComparingTo("80.00");
        assertThat(detected.averageOpportunityR()).isEqualByComparingTo("1.0500");
        assertThat(detected.totalEstimatedOpportunityR()).isEqualByComparingTo("4.20");
    }

    private SimilarTradeResponse similar(Long id, String mistake, String emotion, String r, String mae, String mfe) {
        return new SimilarTradeResponse(
                id,
                LocalDate.of(2026, 7, id.intValue()),
                "NQ",
                "LONG",
                "IFVG",
                r == null ? null : new BigDecimal(r),
                mae == null ? null : new BigDecimal(mae),
                mfe == null ? null : new BigDecimal(mfe),
                mistake,
                emotion,
                0.90,
                "Comparable reviewed trade"
        );
    }
}
