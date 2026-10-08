package com.tradepilot.manualbacktest.service;

import com.tradepilot.manualbacktest.entity.ManualBacktestTrade;
import java.math.BigDecimal;

/** Pure entry-fill rules for one newly revealed OHLC bar. */
final class ManualOrderExecution {
    private ManualOrderExecution() { }

    static BigDecimal entryFill(ManualBacktestTrade.Direction direction, ManualBacktestTrade.EntryOrderType type, BigDecimal configuredEntry, BigDecimal open, BigDecimal high, BigDecimal low) {
        if (type == ManualBacktestTrade.EntryOrderType.MARKET) return open;
        if (type == ManualBacktestTrade.EntryOrderType.LIMIT) {
            if (direction == ManualBacktestTrade.Direction.LONG) return open.compareTo(configuredEntry) <= 0 ? open : low.compareTo(configuredEntry) <= 0 ? configuredEntry : null;
            return open.compareTo(configuredEntry) >= 0 ? open : high.compareTo(configuredEntry) >= 0 ? configuredEntry : null;
        }
        if (direction == ManualBacktestTrade.Direction.LONG) return open.compareTo(configuredEntry) >= 0 ? open : high.compareTo(configuredEntry) >= 0 ? configuredEntry : null;
        return open.compareTo(configuredEntry) <= 0 ? open : low.compareTo(configuredEntry) <= 0 ? configuredEntry : null;
    }
}
