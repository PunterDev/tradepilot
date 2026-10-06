package com.tradepilot.marketdata.service;

import com.tradepilot.marketdata.databento.DatabentoHistoricalClient;
import com.tradepilot.marketdata.databento.DatabentoOhlcvBar;
import com.tradepilot.marketdata.MarketDataException;
import com.tradepilot.marketdata.dto.MarketDataDownloadRequest;
import com.tradepilot.marketdata.dto.MarketDataEstimateRequest;
import com.tradepilot.marketdata.dto.MarketDataCoverageResponse;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalMarketDataServiceTests {
    @Test
    void mapsSupportedInstrumentsToContinuousContracts() {
        assertThat(MarketInstrument.from("NQ").continuousSymbol()).isEqualTo("NQ.v.0");
        assertThat(MarketInstrument.from("es").continuousSymbol()).isEqualTo("ES.v.0");
        assertThatThrownBy(() -> MarketInstrument.from("MNQ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estimateSkipsCompletedMonthAndSumsOnlyMissingRanges() {
        DatabentoHistoricalClient client = mock(DatabentoHistoricalClient.class);
        MarketBarStore store = mock(MarketBarStore.class);
        HistoricalMarketDataService service = new HistoricalMarketDataService(client, store);
        Instant januaryStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant februaryStart = Instant.parse("2026-02-01T00:00:00Z");
        Instant marchStart = Instant.parse("2026-03-01T00:00:00Z");
        when(store.completedChunksOverlapping(MarketInstrument.NQ, januaryStart, marchStart))
                .thenReturn(List.of(new MarketBarStore.CompletedChunk(januaryStart, februaryStart)));
        when(client.estimateCost(MarketInstrument.NQ, februaryStart, marchStart)).thenReturn(new BigDecimal("0.42"));

        var estimate = service.estimate(new MarketDataEstimateRequest("NQ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)));

        assertThat(estimate.continuousSymbol()).isEqualTo("NQ.v.0");
        assertThat(estimate.estimatedCostUsd()).isEqualByComparingTo("0.42");
        assertThat(estimate.missingChunks()).isEqualTo(1);
        verify(client).estimateCost(MarketInstrument.NQ, februaryStart, marchStart);
    }

    @Test
    void downloadChunksMonthlyPersistsInstrumentIdsAndRerunSkipsCompletedChunks() {
        DatabentoHistoricalClient client = mock(DatabentoHistoricalClient.class);
        MarketBarStore store = mock(MarketBarStore.class);
        HistoricalMarketDataService service = new HistoricalMarketDataService(client, store);
        Instant januaryStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant februaryStart = Instant.parse("2026-02-01T00:00:00Z");
        Instant marchStart = Instant.parse("2026-03-01T00:00:00Z");
        when(store.completedChunksOverlapping(any(), any(), any())).thenReturn(List.of());
        when(store.countBars(any(), any(), any())).thenReturn(0L);
        when(store.insertIgnoringDuplicates(eq(MarketInstrument.ES), any())).thenAnswer(invocation -> (long) ((List<?>) invocation.getArgument(1)).size());
        when(store.coverage(MarketInstrument.ES)).thenReturn(new MarketDataCoverageResponse("ES", "ES.v.0", "1m", januaryStart, Instant.parse("2026-02-28T23:59:00Z"), 2));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked") Consumer<DatabentoOhlcvBar> consumer = invocation.getArgument(3);
            Instant chunkStart = invocation.getArgument(1);
            consumer.accept(new DatabentoOhlcvBar(chunkStart, chunkStart.equals(januaryStart) ? 111L : 222L, new BigDecimal("6000"), new BigDecimal("6001"), new BigDecimal("5999"), new BigDecimal("6000.5"), 10));
            return null;
        }).when(client).streamOhlcvBars(eq(MarketInstrument.ES), any(), any(), any());

        var result = service.download(new MarketDataDownloadRequest("ES", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)));

        assertThat(result.downloadedBars()).isEqualTo(2);
        assertThat(result.insertedBars()).isEqualTo(2);
        verify(client, times(2)).streamOhlcvBars(eq(MarketInstrument.ES), any(), any(), any());
        ArgumentCaptor<List<DatabentoOhlcvBar>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(store, times(2)).insertIgnoringDuplicates(eq(MarketInstrument.ES), batchCaptor.capture());
        assertThat(batchCaptor.getAllValues().stream().flatMap(List::stream).map(DatabentoOhlcvBar::instrumentId)).containsExactlyInAnyOrder(111L, 222L);

        when(store.completedChunksOverlapping(any(), any(), any()))
                .thenReturn(List.of(new MarketBarStore.CompletedChunk(januaryStart, marchStart)));
        var rerun = service.download(new MarketDataDownloadRequest("ES", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)));
        assertThat(rerun.downloadedBars()).isZero();
        verify(client, times(2)).streamOhlcvBars(eq(MarketInstrument.ES), any(), any(), any());
    }

    @Test
    void onlyRequestsTheUncoveredPortionOfAnOverlappingPriorImport() {
        DatabentoHistoricalClient client = mock(DatabentoHistoricalClient.class);
        MarketBarStore store = mock(MarketBarStore.class);
        HistoricalMarketDataService service = new HistoricalMarketDataService(client, store);
        Instant januaryStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant januaryFifteenth = Instant.parse("2026-01-15T00:00:00Z");
        Instant februaryStart = Instant.parse("2026-02-01T00:00:00Z");
        when(store.completedChunksOverlapping(MarketInstrument.NQ, januaryStart, februaryStart))
                .thenReturn(List.of(new MarketBarStore.CompletedChunk(januaryFifteenth, februaryStart)));
        when(client.estimateCost(MarketInstrument.NQ, januaryStart, januaryFifteenth)).thenReturn(new BigDecimal("0.25"));

        var estimate = service.estimate(new MarketDataEstimateRequest("NQ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        assertThat(estimate.missingChunks()).isEqualTo(1);
        assertThat(estimate.estimatedCostUsd()).isEqualByComparingTo("0.25");
        verify(client).estimateCost(MarketInstrument.NQ, januaryStart, januaryFifteenth);
    }

    @Test
    void failedStreamDoesNotMarkTheRangeCompleteSoItCanBeRetried() {
        DatabentoHistoricalClient client = mock(DatabentoHistoricalClient.class);
        MarketBarStore store = mock(MarketBarStore.class);
        HistoricalMarketDataService service = new HistoricalMarketDataService(client, store);
        Instant januaryStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant februaryStart = Instant.parse("2026-02-01T00:00:00Z");
        when(store.completedChunksOverlapping(MarketInstrument.NQ, januaryStart, februaryStart)).thenReturn(List.of());
        when(store.countBars(MarketInstrument.NQ, januaryStart, februaryStart)).thenReturn(0L);
        doAnswer(invocation -> { throw new MarketDataException(org.springframework.http.HttpStatus.BAD_GATEWAY, "DATABENTO_STREAM_FAILED", "Databento data download did not complete."); })
                .when(client).streamOhlcvBars(eq(MarketInstrument.NQ), eq(januaryStart), eq(februaryStart), any());

        assertThatThrownBy(() -> service.download(new MarketDataDownloadRequest("NQ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))))
                .isInstanceOf(MarketDataException.class);

        verify(store, never()).recordCompletedChunk(eq(MarketInstrument.NQ), any(), any(), anyLong(), anyLong());
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        HistoricalMarketDataService service = new HistoricalMarketDataService(mock(DatabentoHistoricalClient.class), mock(MarketBarStore.class));
        assertThatThrownBy(() -> service.estimate(new MarketDataEstimateRequest("NQ", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be on or after start date.");
    }
}
