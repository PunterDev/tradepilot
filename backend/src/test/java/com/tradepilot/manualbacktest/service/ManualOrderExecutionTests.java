package com.tradepilot.manualbacktest.service;

import com.tradepilot.manualbacktest.entity.ManualBacktestTrade.Direction;
import com.tradepilot.manualbacktest.entity.ManualBacktestTrade.EntryOrderType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ManualOrderExecutionTests {
    @Test void longLimitWaitsThenFillsAtTheLimit() {
        assertThat(fill(Direction.LONG, EntryOrderType.LIMIT, 100, 110, 111, 101)).isNull();
        assertThat(fill(Direction.LONG, EntryOrderType.LIMIT, 100, 103, 104, 99)).isEqualByComparingTo("100");
    }
    @Test void limitsReceiveFavorableGapImprovement() {
        assertThat(fill(Direction.LONG, EntryOrderType.LIMIT, 100, 98, 102, 97)).isEqualByComparingTo("98");
        assertThat(fill(Direction.SHORT, EntryOrderType.LIMIT, 100, 102, 103, 98)).isEqualByComparingTo("102");
    }
    @Test void stopsFillAtTriggerOrAdverseGap() {
        assertThat(fill(Direction.LONG, EntryOrderType.STOP, 105, 102, 106, 101)).isEqualByComparingTo("105");
        assertThat(fill(Direction.LONG, EntryOrderType.STOP, 105, 107, 108, 106)).isEqualByComparingTo("107");
        assertThat(fill(Direction.SHORT, EntryOrderType.STOP, 95, 98, 99, 94)).isEqualByComparingTo("95");
        assertThat(fill(Direction.SHORT, EntryOrderType.STOP, 95, 93, 94, 92)).isEqualByComparingTo("93");
    }
    @Test void marketOrderUsesOnlyTheNextBarOpen() { assertThat(fill(Direction.LONG, EntryOrderType.MARKET, null, 101, 110, 90)).isEqualByComparingTo("101"); }
    private BigDecimal fill(Direction direction, EntryOrderType type, Integer entry, int open, int high, int low) { return ManualOrderExecution.entryFill(direction,type,entry==null?null:BigDecimal.valueOf(entry),BigDecimal.valueOf(open),BigDecimal.valueOf(high),BigDecimal.valueOf(low)); }
}
