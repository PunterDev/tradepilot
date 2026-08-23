package com.tradepilot.dev;

import com.tradepilot.importing.FuturesPointValueService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDemoDataServiceTests {
    private final FuturesPointValueService pointValueService = new FuturesPointValueService();

    @Test
    void derivesMnqShortWinnerPnlFromCanonicalPointValue() {
        assertThat(DevDemoDataService.calculateGrossPnl(
                "MNQ", "SHORT", new BigDecimal("30000"), new BigDecimal("29990"), 3, pointValueService
        )).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void derivesMnqShortLoserPnlFromCanonicalPointValue() {
        assertThat(DevDemoDataService.calculateGrossPnl(
                "MNQ", "SHORT", new BigDecimal("30000"), new BigDecimal("30010"), 3, pointValueService
        )).isEqualByComparingTo(new BigDecimal("-60.00"));
    }

    @Test
    void derivesMesShortWinnerPnlFromCanonicalPointValue() {
        assertThat(DevDemoDataService.calculateGrossPnl(
                "MES", "SHORT", new BigDecimal("7500"), new BigDecimal("7490"), 3, pointValueService
        )).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void derivesMesShortLoserPnlFromCanonicalPointValue() {
        assertThat(DevDemoDataService.calculateGrossPnl(
                "MES", "SHORT", new BigDecimal("7500"), new BigDecimal("7510"), 3, pointValueService
        )).isEqualByComparingTo(new BigDecimal("-150.00"));
    }

    @Test
    void derivesMnqLongWinnerPnlFromCanonicalPointValue() {
        assertThat(DevDemoDataService.calculateGrossPnl(
                "MNQ", "LONG", new BigDecimal("30000"), new BigDecimal("30010"), 3, pointValueService
        )).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void derivesRMultipleFromFixtureRiskAmount() {
        assertThat(DevDemoDataService.calculateRMultiple(new BigDecimal("150.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(DevDemoDataService.calculateRMultiple(new BigDecimal("-150.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo(new BigDecimal("-1.50"));
    }

    @Test
    void rejectsBrokenMesShortWinnerWithNegativeStoredPnlAndR() {
        assertThatThrownBy(() -> DevDemoDataService.validateTradeMath(
                "MES",
                "SHORT",
                new BigDecimal("5304.50"),
                new BigDecimal("5290.50"),
                3,
                new BigDecimal("-101.25"),
                new BigDecimal("-1.35"),
                pointValueService
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P&L contradicts side/entry/exit math")
                .hasMessageContaining("calculatedPnl=210.00");
    }

    @Test
    void brokenMnqShortExampleDerivesNegativePnlInsteadOfPositiveNinety() {
        BigDecimal derived = DevDemoDataService.calculateGrossPnl(
                "MNQ", "SHORT", new BigDecimal("18863.00"), new BigDecimal("18882.00"), 3, pointValueService
        );

        assertThat(derived).isEqualByComparingTo(new BigDecimal("-114.00"));
        assertThat(derived).isNotEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    void replayCompatibleFixturesAreCalibratedToVerifiedHistoricalCandles() {
        var calibrations = DevDemoDataService.replayCalibrationExpectationsForTesting();

        assertThat(calibrations).hasSize(10);
        assertThat(calibrations).allSatisfy(calibration -> {
            assertThat(calibration.entryCandle().contains(calibration.entryPrice())).isTrue();
            assertThat(calibration.exitCandle().contains(calibration.exitPrice())).isTrue();

            BigDecimal realized = realizedPoints(calibration);
            BigDecimal mfe = "SHORT".equals(calibration.side())
                    ? calibration.entryPrice().subtract(calibration.windowMinLow())
                    : calibration.windowMaxHigh().subtract(calibration.entryPrice());
            BigDecimal mae = "SHORT".equals(calibration.side())
                    ? calibration.windowMaxHigh().subtract(calibration.entryPrice())
                    : calibration.entryPrice().subtract(calibration.windowMinLow());

            assertThat(realized).isEqualByComparingTo(calibration.expectedRealizedPoints());
            assertThat(maxZero(mfe)).isEqualByComparingTo(calibration.expectedMfePoints());
            assertThat(maxZero(mae)).isEqualByComparingTo(calibration.expectedMaePoints());
            assertThat(calibration.pnl()).isEqualByComparingTo(DevDemoDataService.calculateGrossPnl(
                    calibration.symbol(), calibration.side(), calibration.entryPrice(), calibration.exitPrice(), calibration.quantity(), pointValueService
            ));
            assertThat(calibration.rMultiple()).isEqualByComparingTo(
                    calibration.pnl().divide(calibration.riskAmount(), 2, RoundingMode.HALF_UP)
            );
            if (calibration.pnl().signum() != 0) {
                assertThat(calibration.rMultiple().signum()).isEqualTo(calibration.pnl().signum());
            }
        });
    }

    @Test
    void calibratedJunEightMnqFixturePreventsOldHundredsOfPointsExcursion() {
        var calibration = DevDemoDataService.replayCalibrationExpectationsForTesting().stream()
                .filter(candidate -> "MNQM6".equals(candidate.contractSymbol()))
                .filter(candidate -> candidate.tradeDate().equals(java.time.LocalDate.of(2026, 6, 8)))
                .findFirst()
                .orElseThrow();

        assertThat(calibration.entryTime()).isEqualTo(java.time.LocalTime.of(5, 25));
        assertThat(calibration.exitTime()).isEqualTo(java.time.LocalTime.of(5, 28));
        assertThat(calibration.entryPrice()).isEqualByComparingTo(new BigDecimal("29190.00"));
        assertThat(calibration.exitPrice()).isEqualByComparingTo(new BigDecimal("29178.00"));
        assertThat(calibration.expectedRealizedPoints()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(calibration.expectedMfePoints()).isEqualByComparingTo(new BigDecimal("14.25"));
        assertThat(calibration.expectedMaePoints()).isEqualByComparingTo(new BigDecimal("6.25"));
        assertThat(calibration.expectedMfePoints()).isNotEqualByComparingTo(new BigDecimal("843.75"));
    }

    private BigDecimal realizedPoints(DevDemoDataService.DemoReplayCalibration calibration) {
        return "SHORT".equals(calibration.side())
                ? calibration.entryPrice().subtract(calibration.exitPrice())
                : calibration.exitPrice().subtract(calibration.entryPrice());
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }
}
