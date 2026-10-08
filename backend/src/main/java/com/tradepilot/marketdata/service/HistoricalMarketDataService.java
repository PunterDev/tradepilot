package com.tradepilot.marketdata.service;

import com.tradepilot.marketdata.MarketDataException;
import com.tradepilot.marketdata.databento.DatabentoHistoricalClient;
import com.tradepilot.marketdata.databento.DatabentoOhlcvBar;
import com.tradepilot.marketdata.dto.MarketDataDownloadRequest;
import com.tradepilot.marketdata.dto.MarketDataDownloadResponse;
import com.tradepilot.marketdata.dto.MarketDataEstimateRequest;
import com.tradepilot.marketdata.dto.MarketDataEstimateResponse;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class HistoricalMarketDataService {
    private static final Logger log = LoggerFactory.getLogger(HistoricalMarketDataService.class);
    private static final int BATCH_SIZE = 1_000;
    private final DatabentoHistoricalClient databentoClient;
    private final MarketBarStore marketBarStore;
    private final ConcurrentHashMap<MarketInstrument, ReentrantLock> importLocks = new ConcurrentHashMap<>();

    public HistoricalMarketDataService(DatabentoHistoricalClient databentoClient, MarketBarStore marketBarStore) {
        this.databentoClient = databentoClient;
        this.marketBarStore = marketBarStore;
    }

    public MarketDataEstimateResponse estimate(MarketDataEstimateRequest request) {
        RequestWindow window = window(request.instrument(), request.startDate(), request.endDate());
        List<Chunk> missingChunks = missingChunks(window);
        BigDecimal cost = BigDecimal.ZERO;
        for (Chunk range : contiguousRanges(missingChunks)) {
            cost = cost.add(databentoClient.estimateCost(window.instrument(), range.start(), range.end()));
        }
        return new MarketDataEstimateResponse(window.instrument().name(), window.instrument().continuousSymbol(), window.startDate(), window.endDate(), cost, window.requestedDays(), missingChunks.size());
    }

    public MarketDataDownloadResponse download(MarketDataDownloadRequest request) {
        RequestWindow window = window(request.instrument(), request.startDate(), request.endDate());
        ReentrantLock lock = importLocks.computeIfAbsent(window.instrument(), ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new MarketDataException(HttpStatus.CONFLICT, "MARKET_DATA_IMPORT_RUNNING", "A historical data import for this instrument is already running.");
        }
        try {
            List<Chunk> missingChunks = missingChunks(window);
            long skippedExisting = marketBarStore.countBars(window.instrument(), window.start(), window.end());
            long downloaded = 0;
            long inserted = 0;
            log.info("Starting Databento historical import. instrument={}, continuousSymbol={}, start={}, end={}, missingChunks={}",
                    window.instrument(), window.instrument().continuousSymbol(), window.start(), window.end(), missingChunks.size());
            for (Chunk chunk : missingChunks) {
                BatchAccumulator accumulator = new BatchAccumulator(window.instrument());
                log.info("Databento historical import chunk. instrument={}, start={}, end={}", window.instrument(), chunk.start(), chunk.end());
                databentoClient.streamOhlcvBars(window.instrument(), chunk.start(), chunk.end(), accumulator::accept);
                accumulator.flush();
                downloaded += accumulator.received();
                inserted += accumulator.inserted();
                marketBarStore.recordCompletedChunk(window.instrument(), chunk.start(), chunk.end(), accumulator.received(), accumulator.inserted());
                log.info("Databento historical import chunk completed. instrument={}, start={}, end={}, received={}, inserted={}, skipped={}",
                        window.instrument(), chunk.start(), chunk.end(), accumulator.received(), accumulator.inserted(), accumulator.received() - accumulator.inserted());
            }
            var coverage = marketBarStore.coverage(window.instrument());
            log.info("Databento historical import completed. instrument={}, downloaded={}, inserted={}, skippedExisting={}",
                    window.instrument(), downloaded, inserted, skippedExisting + downloaded - inserted);
            return new MarketDataDownloadResponse(window.instrument().name(), window.instrument().continuousSymbol(), window.startDate(), window.endDate(),
                    downloaded, skippedExisting + downloaded - inserted, inserted, coverage.firstBar(), coverage.lastBar(), "COMPLETED");
        } finally {
            lock.unlock();
        }
    }

    private RequestWindow window(String instrumentValue, LocalDate startDate, LocalDate endDate) {
        MarketInstrument instrument = MarketInstrument.from(instrumentValue);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }
        Instant start = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new RequestWindow(instrument, startDate, endDate, start, end, ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    private List<Chunk> missingChunks(RequestWindow window) {
        List<MarketBarStore.CompletedChunk> completed = marketBarStore.completedChunksOverlapping(window.instrument(), window.start(), window.end());
        List<Chunk> missing = new ArrayList<>();
        Instant cursor = window.start();
        for (MarketBarStore.CompletedChunk chunk : completed) {
            Instant coveredStart = chunk.start().isAfter(window.start()) ? chunk.start() : window.start();
            Instant coveredEnd = chunk.end().isBefore(window.end()) ? chunk.end() : window.end();
            if (!coveredEnd.isAfter(cursor)) {
                continue;
            }
            if (coveredStart.isAfter(cursor)) {
                missing.addAll(monthlyChunks(cursor, coveredStart));
            }
            cursor = coveredEnd;
        }
        if (cursor.isBefore(window.end())) {
            missing.addAll(monthlyChunks(cursor, window.end()));
        }
        return missing;
    }

    private List<Chunk> monthlyChunks(Instant start, Instant end) {
        List<Chunk> chunks = new ArrayList<>();
        Instant cursor = start;
        while (cursor.isBefore(end)) {
            LocalDate date = cursor.atZone(ZoneOffset.UTC).toLocalDate();
            Instant nextMonth = date.withDayOfMonth(1).plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant chunkEnd = nextMonth.isBefore(end) ? nextMonth : end;
            chunks.add(new Chunk(cursor, chunkEnd));
            cursor = chunkEnd;
        }
        return chunks;
    }

    private List<Chunk> contiguousRanges(List<Chunk> chunks) {
        List<Chunk> ranges = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (ranges.isEmpty() || !ranges.get(ranges.size() - 1).end().equals(chunk.start())) {
                ranges.add(chunk);
            } else {
                Chunk previous = ranges.remove(ranges.size() - 1);
                ranges.add(new Chunk(previous.start(), chunk.end()));
            }
        }
        return ranges;
    }

    private final class BatchAccumulator {
        private final MarketInstrument instrument;
        private final List<DatabentoOhlcvBar> pending = new ArrayList<>(BATCH_SIZE);
        private long received;
        private long inserted;

        private BatchAccumulator(MarketInstrument instrument) {
            this.instrument = instrument;
        }

        private void accept(DatabentoOhlcvBar bar) {
            received++;
            pending.add(bar);
            if (pending.size() == BATCH_SIZE) flush();
        }

        private void flush() {
            if (pending.isEmpty()) return;
            inserted += marketBarStore.insertIgnoringDuplicates(instrument, List.copyOf(pending));
            pending.clear();
        }

        private long received() { return received; }
        private long inserted() { return inserted; }
    }

    private record RequestWindow(MarketInstrument instrument, LocalDate startDate, LocalDate endDate, Instant start, Instant end, long requestedDays) {
    }

    private record Chunk(Instant start, Instant end) {
    }
}
