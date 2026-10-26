package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportPreviewResponse;
import com.tradepilot.dto.trade.TradeImportPreviewTradeResponse;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeImportSaveResponse;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.datatrust.DataTrustService;
import com.tradepilot.tradereplay.service.TradeReplayService;
import com.tradepilot.survival.AccountSurvivalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TradeCsvImportService {
    private static final long MAX_CSV_BYTES = 2_000_000L;

    private final TopstepCsvParser topstepCsvParser;
    private final TradovateCsvParser tradovateCsvParser;
    private final TradingAccountService tradingAccountService;
    private final TradeRepository tradeRepository;
    private final TradeService tradeService;
    private final TradeReplayService tradeReplayService;
    private final FeatureUsageService featureUsageService;
    private final FeatureUsageRepository featureUsageRepository;
    private final SeoAnalyticsService seoAnalyticsService;
    private final ImportBatchRepository importBatchRepository;
    private final DataTrustService dataTrustService;
    private final AccountSurvivalService accountSurvivalService;
    private final ObjectMapper objectMapper;

    public TradeCsvImportService(
            TopstepCsvParser topstepCsvParser,
            TradovateCsvParser tradovateCsvParser,
            TradingAccountService tradingAccountService,
            TradeRepository tradeRepository,
            TradeService tradeService,
            TradeReplayService tradeReplayService,
            FeatureUsageService featureUsageService,
            FeatureUsageRepository featureUsageRepository,
            SeoAnalyticsService seoAnalyticsService, ImportBatchRepository importBatchRepository, DataTrustService dataTrustService, ObjectMapper objectMapper, AccountSurvivalService accountSurvivalService
    ) {
        this.topstepCsvParser = topstepCsvParser;
        this.tradovateCsvParser = tradovateCsvParser;
        this.tradingAccountService = tradingAccountService;
        this.tradeRepository = tradeRepository;
        this.tradeService = tradeService;
        this.tradeReplayService = tradeReplayService;
        this.featureUsageService = featureUsageService;
        this.featureUsageRepository = featureUsageRepository;
        this.seoAnalyticsService = seoAnalyticsService;
        this.importBatchRepository = importBatchRepository;
        this.dataTrustService = dataTrustService;
        this.objectMapper = objectMapper;
        this.accountSurvivalService = accountSurvivalService;
    }

    public TradeImportPreviewResponse preview(User user, Long accountId, MultipartFile file) {
        featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_STARTED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", ImportTelemetry.failure(objectMapper, "preview", ImportTelemetry.ParserSource.UNKNOWN, ImportTelemetry.FailureCategory.UNKNOWN));
        try {
            ParsedImport parsed = parse(user, accountId, file);
            return toPreview(user, accountId, parsed);
        } catch (RuntimeException ex) {
            recordFailure(user, accountId, file, "preview", ex);
            throw ex;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public TradeImportSaveResponse save(User user, Long accountId, MultipartFile file) {
        boolean firstImportAlreadyTracked = featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.FIRST_IMPORT_COMPLETED);
        ParsedImport parsed = null;
        try {
            parsed = parse(user, accountId, file);
            TradeImportPreviewResponse preview = toPreview(user, accountId, parsed);
            List<TradeResponse> saved = parsed.trades().isEmpty()
                    ? List.of()
                    : tradeService.importBrokerTrades(user, parsed.trades());
            ParsedImport completedParsed = parsed;
            Long importId = saved.isEmpty() ? null : importBatchRepository.findTopByUserAndAccount_IdOrderByImportedAtDesc(user, accountId).map(batch -> dataTrustService.createForImport(user, batch, completedParsed.format().name()).importId()).orElse(null);
            tradeReplayService.initializeAfterCommit(tradeService.getImportedTradesForReplayInitialization(user, saved.stream().map(TradeResponse::getId).toList()));
            if (!saved.isEmpty()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { accountSurvivalService.evaluateEligibleAfterImport(user, accountId); }
                });
            }
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_COMPLETED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", ImportTelemetry.completed(objectMapper, parsed, saved.size(), preview.duplicateTrades()));
            if (!firstImportAlreadyTracked && !saved.isEmpty()) {
                featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.FIRST_IMPORT_COMPLETED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", ImportTelemetry.firstImport(objectMapper, parsed, saved.size()));
                seoAnalyticsService.markFirstImportCompleted(user, null);
            }
            return new TradeImportSaveResponse(preview, saved, saved.size(), importId);
        } catch (RuntimeException ex) {
            recordFailure(user, accountId, file, "save", parsed == null ? ex : new ImportTelemetryException(ex.getMessage(), ImportTelemetry.sourceFor(parsed), ImportTelemetry.FailureCategory.SAVE_ERROR, ex));
            throw ex;
        }
    }

    private ParsedImport parse(User user, Long accountId, MultipartFile file) {
        if (accountId == null) {
            throw telemetryError("accountId is required for CSV import.", ImportTelemetry.FailureCategory.ACCOUNT_VALIDATION);
        }
        try { tradingAccountService.getAccountForImport(user, accountId); } catch (RuntimeException ex) { throw telemetryError(ex.getMessage(), ImportTelemetry.FailureCategory.ACCOUNT_VALIDATION, ex); }
        if (file == null || file.isEmpty()) {
            throw telemetryError("CSV file is required.", ImportTelemetry.FailureCategory.FILE_REQUIRED);
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw telemetryError("CSV file is too large. Upload a file under 2 MB.", ImportTelemetry.FailureCategory.FILE_TOO_LARGE);
        }
        String csv = readCsv(file);
        CsvTable table = CsvTable.parse(csv);
        boolean supportsTopstep = topstepCsvParser.supports(table);
        boolean supportsTradovate = tradovateCsvParser.supports(table);
        if (supportsTopstep && supportsTradovate) {
            throw telemetryError("CSV format is ambiguous. Export a clean Tradovate or Topstep CSV and try again.", ImportTelemetry.FailureCategory.AMBIGUOUS_FORMAT);
        }
        if (supportsTopstep) {
            return parseSelected(() -> topstepCsvParser.parse(csv, accountId, safeFileName(file)), ImportTelemetry.ParserSource.TOPSTEP);
        }
        if (supportsTradovate) {
            return parseSelected(() -> tradovateCsvParser.parse(csv, accountId, safeFileName(file)), ImportTelemetry.ParserSource.TRADOVATE);
        }
        throw telemetryError("Unsupported CSV format. TradePilot currently supports Tradovate dashboard CSVs and Topstep order-history CSVs.", ImportTelemetry.FailureCategory.UNSUPPORTED_FORMAT);
    }

    private TradeImportPreviewResponse toPreview(User user, Long accountId, ParsedImport parsed) {
        List<String> keys = parsed.trades().stream()
                .map(TradeImportRequest::externalTradeKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        Set<String> existingKeys = keys.isEmpty()
                ? Set.of()
                : new HashSet<>(tradeRepository.findExistingExternalTradeKeys(user, accountId, keys));

        List<TradeImportPreviewTradeResponse> previewTrades = new java.util.ArrayList<>();
        int duplicates = 0;
        for (int i = 0; i < parsed.trades().size(); i++) {
            TradeImportRequest request = parsed.trades().get(i);
            boolean duplicate = request.externalTradeKey() != null && existingKeys.contains(request.externalTradeKey());
            if (duplicate) {
                duplicates++;
            }
            previewTrades.add(new TradeImportPreviewTradeResponse(
                    request.symbol(),
                    request.side(),
                    request.tradeDate(),
                    request.entryTime(),
                    request.exitTime(),
                    request.quantity(),
                    request.entryPrice(),
                    request.exitPrice(),
                    request.pnlAmount(),
                    request.originalBrokerSymbol(),
                    request.contractSymbol(),
                    request.externalTradeKey(),
                    i < parsed.sourceExecutionIds().size() ? parsed.sourceExecutionIds().get(i) : List.of(),
                    duplicate
            ));
        }

        return new TradeImportPreviewResponse(
                parsed.format(),
                parsed.sourceAccountNames(),
                parsed.totalRows(),
                parsed.filledRows(),
                parsed.cancelledRowsIgnored(),
                parsed.invalidRows(),
                parsed.trades().size(),
                duplicates,
                parsed.incompletePositions(),
                parsed.warnings(),
                previewTrades
        );
    }

    private String readCsv(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw telemetryError("Unable to read CSV file.", ImportTelemetry.FailureCategory.FILE_READ_FAILED, ex);
        }
    }

    private ParsedImport parseSelected(java.util.concurrent.Callable<ParsedImport> parser, ImportTelemetry.ParserSource source) {
        try { return parser.call(); } catch (ImportTelemetryException ex) { throw ex; } catch (Exception ex) { throw telemetryError(ex.getMessage(), source, ImportTelemetry.FailureCategory.PARSER_ERROR, ex); }
    }

    private void recordFailure(User user, Long accountId, MultipartFile file, String stage, RuntimeException ex) {
        ImportTelemetry.ParserSource source = ex instanceof ImportTelemetryException telemetry ? telemetry.parserSource() : ImportTelemetry.ParserSource.UNKNOWN;
        ImportTelemetry.FailureCategory category = ex instanceof ImportTelemetryException telemetry ? telemetry.failureCategory() : ImportTelemetry.FailureCategory.UNKNOWN;
        featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_FAILED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", ImportTelemetry.failure(objectMapper, stage, source, category));
    }

    private ImportTelemetryException telemetryError(String message, ImportTelemetry.FailureCategory category) { return telemetryError(message, ImportTelemetry.ParserSource.UNKNOWN, category, null); }
    private ImportTelemetryException telemetryError(String message, ImportTelemetry.FailureCategory category, Throwable cause) { return telemetryError(message, ImportTelemetry.ParserSource.UNKNOWN, category, cause); }
    private ImportTelemetryException telemetryError(String message, ImportTelemetry.ParserSource source, ImportTelemetry.FailureCategory category, Throwable cause) { return new ImportTelemetryException(message, source, category, cause); }

    private String safeFileName(MultipartFile file) {
        if (file == null) {
            return "upload.csv";
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "upload.csv";
        }
        return name.replaceAll("[\\\\/]", "_");
    }
}
