package com.tradepilot.marketdata.repository;

import com.tradepilot.marketdata.databento.DatabentoOhlcvBar;
import com.tradepilot.marketdata.dto.MarketDataCoverageResponse;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class MarketBarStore {
    private static final String TIMEFRAME = "1m";
    private static final String SOURCE = "DATABENTO";
    private final JdbcTemplate jdbcTemplate;

    public MarketBarStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertIgnoringDuplicates(MarketInstrument instrument, List<DatabentoOhlcvBar> bars) {
        if (bars.isEmpty()) return 0;
        int[][] updates = jdbcTemplate.batchUpdate("""
                insert into market_bars
                (symbol, continuous_symbol, source, instrument_id, timeframe, ts_event, open, high, low, close, volume, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (continuous_symbol, timeframe, ts_event) do nothing
                """, bars, bars.size(), (statement, bar) -> {
            statement.setString(1, instrument.name());
            statement.setString(2, instrument.continuousSymbol());
            statement.setString(3, SOURCE);
            statement.setLong(4, bar.instrumentId());
            statement.setString(5, TIMEFRAME);
            statement.setTimestamp(6, Timestamp.from(bar.tsEvent()));
            statement.setBigDecimal(7, bar.open());
            statement.setBigDecimal(8, bar.high());
            statement.setBigDecimal(9, bar.low());
            statement.setBigDecimal(10, bar.close());
            statement.setLong(11, bar.volume());
            statement.setTimestamp(12, Timestamp.from(Instant.now()));
        });
        long inserted = 0;
        for (int[] batch : updates) {
            for (int update : batch) if (update > 0) inserted += update;
        }
        return inserted;
    }

    public List<CompletedChunk> completedChunksOverlapping(MarketInstrument instrument, Instant start, Instant end) {
        return jdbcTemplate.query("""
                select chunk_start, chunk_end
                from market_data_import_chunks
                where continuous_symbol = ? and timeframe = ? and chunk_start < ? and chunk_end > ?
                order by chunk_start
                """, (rs, row) -> new CompletedChunk(
                rs.getTimestamp("chunk_start").toInstant(),
                rs.getTimestamp("chunk_end").toInstant()
        ), instrument.continuousSymbol(), TIMEFRAME, Timestamp.from(end), Timestamp.from(start));
    }

    public void recordCompletedChunk(MarketInstrument instrument, Instant start, Instant end, long received, long inserted) {
        jdbcTemplate.update("""
                insert into market_data_import_chunks
                (symbol, continuous_symbol, timeframe, chunk_start, chunk_end, records_received, inserted_bars, completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (continuous_symbol, timeframe, chunk_start, chunk_end) do nothing
                """, instrument.name(), instrument.continuousSymbol(), TIMEFRAME, Timestamp.from(start), Timestamp.from(end), received, inserted, Timestamp.from(Instant.now()));
    }

    public long countBars(MarketInstrument instrument, Instant start, Instant end) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from market_bars
                where continuous_symbol = ? and timeframe = ? and ts_event >= ? and ts_event < ?
                """, Long.class, instrument.continuousSymbol(), TIMEFRAME, Timestamp.from(start), Timestamp.from(end));
        return count == null ? 0 : count;
    }

    public MarketDataCoverageResponse coverage(MarketInstrument instrument) {
        return jdbcTemplate.queryForObject("""
                select min(ts_event) as first_bar, max(ts_event) as last_bar, count(*) as bar_count
                from market_bars where continuous_symbol = ? and timeframe = ?
                """, (rs, row) -> new MarketDataCoverageResponse(
                instrument.name(), instrument.continuousSymbol(), TIMEFRAME,
                rs.getTimestamp("first_bar") == null ? null : rs.getTimestamp("first_bar").toInstant(),
                rs.getTimestamp("last_bar") == null ? null : rs.getTimestamp("last_bar").toInstant(),
                rs.getLong("bar_count")
        ), instrument.continuousSymbol(), TIMEFRAME);
    }

    public record CompletedChunk(Instant start, Instant end) {
    }
}
