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

    public TradeCsvImportService(
            TopstepCsvParser topstepCsvParser,
            TradovateCsvParser tradovateCsvParser,
            TradingAccountService tradingAccountService,
            TradeRepository tradeRepository,
            TradeService tradeService,
            TradeReplayService tradeReplayService,
            FeatureUsageService featureUsageService,
            FeatureUsageRepository featureUsageRepository,
            SeoAnalyticsService seoAnalyticsService, ImportBatchRepository importBatchRepository, DataTrustService dataTrustService
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
    }

    public TradeImportPreviewResponse preview(User user, Long accountId, MultipartFile file) {
        featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_STARTED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", null);
        try {
            ParsedImport parsed = parse(user, accountId, file);
            return toPreview(user, accountId, parsed);
        } catch (RuntimeException ex) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_FAILED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", "{\"stage\":\"preview\"}");
            throw ex;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public TradeImportSaveResponse save(User user, Long accountId, MultipartFile file) {
        boolean firstImportAlreadyTracked = featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.FIRST_IMPORT_COMPLETED);
        try {
            ParsedImport parsed = parse(user, accountId, file);
            TradeImportPreviewResponse preview = toPreview(user, accountId, parsed);
            List<TradeResponse> saved = parsed.trades().isEmpty()
                    ? List.of()
                    : tradeService.importBrokerTrades(user, parsed.trades());
            Long importId = saved.isEmpty() ? null : importBatchRepository.findTopByUserAndAccount_IdOrderByImportedAtDesc(user, accountId).map(batch -> dataTrustService.createForImport(user, batch, parsed.format().name()).importId()).orElse(null);
            tradeReplayService.initializeAfterCommit(tradeService.getImportedTradesForReplayInitialization(user, saved.stream().map(TradeResponse::getId).toList()));
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_COMPLETED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", "{\"savedRows\":" + saved.size() + "}");
            if (!firstImportAlreadyTracked && !saved.isEmpty()) {
                featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.FIRST_IMPORT_COMPLETED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", "{\"savedRows\":" + saved.size() + "}");
                seoAnalyticsService.markFirstImportCompleted(user, null);
            }
            return new TradeImportSaveResponse(preview, saved, saved.size(), importId);
        } catch (RuntimeException ex) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.CSV_IMPORT_FAILED, FeatureUsageEventType.USER_ACTION, accountId, "CSV_IMPORT", safeFileName(file), "csv_import", "{\"stage\":\"save\"}");
            throw ex;
        }
    }

    private ParsedImport parse(User user, Long accountId, MultipartFile file) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for CSV import.");
        }
        tradingAccountService.getAccountForImport(user, accountId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required.");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw new IllegalArgumentException("CSV file is too large. Upload a file under 2 MB.");
        }
        String csv = readCsv(file);
        CsvTable table = CsvTable.parse(csv);
        boolean supportsTopstep = topstepCsvParser.supports(table);
        boolean supportsTradovate = tradovateCsvParser.supports(table);
        if (supportsTopstep && supportsTradovate) {
            throw new IllegalArgumentException("CSV format is ambiguous. Export a clean Tradovate or Topstep CSV and try again.");
        }
        if (supportsTopstep) {
            return topstepCsvParser.parse(csv, accountId, safeFileName(file));
        }
        if (supportsTradovate) {
            return tradovateCsvParser.parse(csv, accountId, safeFileName(file));
        }
        throw new IllegalArgumentException("Unsupported CSV format. TradePilot currently supports Tradovate dashboard CSVs and Topstep order-history CSVs.");
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
            throw new IllegalArgumentException("Unable to read CSV file.");
        }
    }

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
