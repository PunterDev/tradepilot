package com.tradepilot.importing;

import com.tradepilot.enums.SessionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class TradingSessionClassifierTests {
    private final TradingSessionClassifier classifier = new TradingSessionClassifier();

    @Test
    void canonicalMarketTimezoneIsNewYork() {
        assertThat(TradingSessionPolicy.MARKET_ZONE).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    void tradovateBrokerTimezoneIsChicago() {
        assertThat(TradingSessionPolicy.TRADOVATE_BROKER_ZONE).isEqualTo(ZoneId.of("America/Chicago"));
    }

    @Test
    void classifiesByExecutionInstantInCanonicalMarketTime() {
        OffsetDateTime chicagoClockTime = OffsetDateTime.parse("2026-07-15T08:35:00-05:00");
        assertThat(classifier.classify(chicagoClockTime)).isEqualTo(SessionType.NY_AM);

        OffsetDateTime sameWallClockInNewYork = OffsetDateTime.parse("2026-07-15T08:35:00-04:00");
        assertThat(classifier.classify(sameWallClockInNewYork)).isEqualTo(SessionType.LONDON);
    }

    @Test
    void classifiesExplicitMarketLocalTimestampsForOffsetlessTradovateRows() {
        assertThat(classifier.classifyMarketLocalTimestamp(LocalDateTime.parse("2026-06-16T09:45:00"))).isEqualTo(SessionType.NY_AM);
        assertThat(classifier.classifyMarketLocalTimestamp(LocalDateTime.parse("2026-06-16T13:15:00"))).isEqualTo(SessionType.NY_PM);
    }

    @Test
    void classifiesSourceLocalTimestampsThroughCanonicalMarketZone() {
        assertThat(classifier.classifySourceLocalTimestamp(
                LocalDateTime.parse("2026-09-03T09:16:00"),
                TradingSessionPolicy.TRADOVATE_BROKER_ZONE
        )).isEqualTo(SessionType.NY_AM);
        assertThat(LocalDateTime.parse("2026-09-03T09:16:00")
                .atZone(TradingSessionPolicy.TRADOVATE_BROKER_ZONE)
                .toInstant()
                .atZone(TradingSessionPolicy.MARKET_ZONE)
                .toLocalDateTime()).hasToString("2026-09-03T10:16");
    }

    @Test
    void classifiesSummerAndWinterCentralSourceTimestampsWithIanaDstRules() {
        assertThat(classifier.classifySourceLocalTimestamp(
                LocalDateTime.parse("2026-07-15T08:35:00"),
                TradingSessionPolicy.TRADOVATE_BROKER_ZONE
        )).isEqualTo(SessionType.NY_AM);
        assertThat(classifier.classifySourceLocalTimestamp(
                LocalDateTime.parse("2026-01-15T08:35:00"),
                TradingSessionPolicy.TRADOVATE_BROKER_ZONE
        )).isEqualTo(SessionType.NY_AM);
    }

    @Test
    void classifiesCentralSourceDstBoundaryUsingRegionRules() {
        assertThat(classifier.classifySourceLocalTimestamp(
                LocalDateTime.parse("2026-03-08T00:59:59"),
                TradingSessionPolicy.TRADOVATE_BROKER_ZONE
        )).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classifySourceLocalTimestamp(
                LocalDateTime.parse("2026-03-08T03:00:00"),
                TradingSessionPolicy.TRADOVATE_BROKER_ZONE
        )).isEqualTo(SessionType.LONDON);
    }

    @Test
    void classifiesOvernightAndLondonSessions() {
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-15T19:05:00-04:00"))).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-16T04:30:00-04:00"))).isEqualTo(SessionType.LONDON);
    }

    @Test
    void usesStartInclusiveEndExclusiveBoundaries() {
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-16T17:59:59-04:00"))).isEqualTo(SessionType.UNKNOWN);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-16T18:00:00-04:00"))).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T02:59:59-04:00"))).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T03:00:00-04:00"))).isEqualTo(SessionType.LONDON);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T09:29:59-04:00"))).isEqualTo(SessionType.LONDON);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T09:30:00-04:00"))).isEqualTo(SessionType.NY_AM);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T12:29:59-04:00"))).isEqualTo(SessionType.NY_AM);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T12:30:00-04:00"))).isEqualTo(SessionType.NY_PM);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T15:59:59-04:00"))).isEqualTo(SessionType.NY_PM);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-17T16:00:00-04:00"))).isEqualTo(SessionType.UNKNOWN);
    }

    @Test
    void handlesSpringDstTransitionThroughInstantConversion() {
        assertThat(classifier.classify(OffsetDateTime.parse("2026-03-08T01:59:59-05:00"))).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-03-08T03:00:00-04:00"))).isEqualTo(SessionType.LONDON);
    }

    @Test
    void handlesFallDstRepeatedHourThroughInstantConversion() {
        assertThat(classifier.classify(OffsetDateTime.parse("2026-11-01T01:30:00-04:00"))).isEqualTo(SessionType.ASIA);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-11-01T01:30:00-05:00"))).isEqualTo(SessionType.ASIA);
        assertThat(OffsetDateTime.parse("2026-11-01T01:30:00-04:00").toInstant())
                .isNotEqualTo(OffsetDateTime.parse("2026-11-01T01:30:00-05:00").toInstant());
    }

    @Test
    void classificationIsIndependentOfJvmDefaultTimezone() {
        TimeZone original = TimeZone.getDefault();
        Instant instant = OffsetDateTime.parse("2026-07-15T13:35:00Z").toInstant();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            SessionType utc = classifier.classify(instant);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
            SessionType chicago = classifier.classify(instant);
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kathmandu"));
            SessionType kathmandu = classifier.classify(instant);

            assertThat(utc).isEqualTo(SessionType.NY_AM);
            assertThat(chicago).isEqualTo(utc);
            assertThat(kathmandu).isEqualTo(utc);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void returnsUnknownForNullOrUnmappedTime() {
        assertThat(classifier.classify((Instant) null)).isEqualTo(SessionType.UNKNOWN);
        assertThat(classifier.classify((OffsetDateTime) null)).isEqualTo(SessionType.UNKNOWN);
        assertThat(classifier.classifyMarketLocalTimestamp(null)).isEqualTo(SessionType.UNKNOWN);
        assertThat(classifier.classify(OffsetDateTime.parse("2026-06-16T17:15:00-04:00"))).isEqualTo(SessionType.UNKNOWN);
    }
}
