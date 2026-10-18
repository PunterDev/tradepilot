package com.tradepilot.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.datatrust.DataTrustService;
import com.tradepilot.dto.trade.TradeImportFormat;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
import com.tradepilot.tradereplay.service.TradeReplayService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TradeCsvImportServiceTelemetryTests {
    @Test
    void successfulSavePersistsDetectedParserAndSafeCounts() {
        TopstepCsvParser topstep = mock(TopstepCsvParser.class);
        TradovateCsvParser tradovate = mock(TradovateCsvParser.class);
        when(topstep.supports(any())).thenReturn(true);
        when(tradovate.supports(any())).thenReturn(false);
        when(topstep.parse(anyString(), anyLong(), anyString())).thenReturn(parsed(TradeImportFormat.TOPSTEP));
        List<String> events = trackedEvents();
        TradeCsvImportService service = service(topstep, tradovate, events);

        service.save(user(), 7L, file());

        assertThat(events).anyMatch(event -> event.contains("CSV_IMPORT_COMPLETED") && event.contains("\"parserSource\":\"TOPSTEP\"") && event.contains("\"totalRows\":3") && event.contains("\"parsedTrades\":0") && event.contains("\"savedRows\":0") && event.contains("\"duplicateRows\":0"));
    }

    @Test
    void unsupportedAndSelectedParserFailuresRetainSafeSourceAndCategory() {
        TopstepCsvParser topstep = mock(TopstepCsvParser.class);
        TradovateCsvParser tradovate = mock(TradovateCsvParser.class);
        when(topstep.supports(any())).thenReturn(false);
        when(tradovate.supports(any())).thenReturn(false);
        List<String> unsupported = trackedEvents();
        assertThatThrownBy(() -> service(topstep, tradovate, unsupported).preview(user(), 7L, file())).isInstanceOf(IllegalArgumentException.class);
        assertThat(unsupported).anyMatch(event -> event.contains("CSV_IMPORT_FAILED") && event.contains("\"parserSource\":\"UNKNOWN\"") && event.contains("\"failureCategory\":\"UNSUPPORTED_FORMAT\"") && event.contains("\"stage\":\"preview\""));

        reset(topstep, tradovate);
        when(topstep.supports(any())).thenReturn(true);
        when(topstep.parse(anyString(), anyLong(), anyString())).thenThrow(new IllegalArgumentException("raw parser message"));
        List<String> parserFailure = trackedEvents();
        assertThatThrownBy(() -> service(topstep, tradovate, parserFailure).preview(user(), 7L, file())).isInstanceOf(IllegalArgumentException.class);
        assertThat(parserFailure).anyMatch(event -> event.contains("CSV_IMPORT_FAILED") && event.contains("\"parserSource\":\"TOPSTEP\"") && event.contains("\"failureCategory\":\"PARSER_ERROR\"") && !event.contains("raw parser message"));
    }

    private static TradeCsvImportService service(TopstepCsvParser topstep, TradovateCsvParser tradovate, List<String> events) {
        FeatureUsageService usage = mock(FeatureUsageService.class);
        doAnswer(invocation -> { events.add(invocation.getArgument(2).toString() + ":" + invocation.getArgument(8)); return null; }).when(usage).track(any(), any(), any(), any(), any(), any(), any(), any(), any());
        FeatureUsageRepository eventRepository = mock(FeatureUsageRepository.class);
        when(eventRepository.existsByUserIdAndAction(anyLong(), any())).thenReturn(false);
        return new TradeCsvImportService(topstep, tradovate, mock(TradingAccountService.class), mock(TradeRepository.class), mock(TradeService.class), mock(TradeReplayService.class), usage, eventRepository, mock(SeoAnalyticsService.class), mock(ImportBatchRepository.class), mock(DataTrustService.class), new ObjectMapper());
    }

    private static ParsedImport parsed(TradeImportFormat format) { return new ParsedImport(format, List.of(), 3, 3, 0, 0, 0, List.of(), List.of(), List.of()); }
    private static User user() { User user = new User(); user.setId(9L); return user; }
    private static MockMultipartFile file() { return new MockMultipartFile("file", "upload.csv", "text/csv", "header\nvalue".getBytes()); }
    private static List<String> trackedEvents() { return new ArrayList<>(); }
}
