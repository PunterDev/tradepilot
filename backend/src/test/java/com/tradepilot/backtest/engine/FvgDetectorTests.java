package com.tradepilot.backtest.engine;
import com.tradepilot.backtest.model.*; import org.junit.jupiter.api.Test; import java.math.*; import java.time.*; import static org.assertj.core.api.Assertions.*;
class FvgDetectorTests { private final FvgDetector detector=new FvgDetector(); private BacktestBar bar(int n,String h,String l){return new BacktestBar(Instant.parse("2026-09-08T14:0"+n+":00Z"),1,new BigDecimal(l),new BigDecimal(h),new BigDecimal(l),new BigDecimal(h),1);}
 @Test void detectsBullishFvgOnlyWhenThirdCandleHasClosed(){var zone=detector.detect(bar(0,"100","90"),bar(1,"105","95"),bar(2,"110","102"),BigDecimal.ONE);assertThat(zone).hasValueSatisfying(z->{assertThat(z.direction()).isEqualTo(PriceZone.Direction.BULLISH);assertThat(z.low()).isEqualByComparingTo("100");assertThat(z.high()).isEqualByComparingTo("102");});}
 @Test void rejectsContractRolloverGap(){BacktestBar first=bar(0,"100","90"),middle=bar(1,"105","95"),third=new BacktestBar(Instant.now(),2,new BigDecimal("102"),new BigDecimal("110"),new BigDecimal("102"),new BigDecimal("110"),1);assertThat(detector.detect(first,middle,third,BigDecimal.ONE)).isEmpty();}
}
