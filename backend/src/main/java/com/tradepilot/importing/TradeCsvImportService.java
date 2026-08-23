package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportPreviewResponse;
import com.tradepilot.dto.trade.TradeImportPreviewTradeResponse;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.dto.trade.TradeImportSaveResponse;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.User;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.service.TradeService;
import com.tradepilot.service.TradingAccountService;
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

    public TradeCsvImportService(
            TopstepCsvParser topstepCsvParser,
            TradovateCsvParser tradovateCsvParser,
            TradingAccountService tradingAccountService,
            TradeRepository tradeRepository,
            TradeService tradeService,
            TradeReplayService tradeReplayService
    ) {
        this.topstepCsvParser = topstepCsvParser;
        this.tradovateCsvParser = tradovateCsvParser;
        this.tradingAccountService = tradingAccountService;
        this.tradeRepository = tradeRepository;
        this.tradeService = tradeService;
        this.tradeReplayService = tradeReplayService;
    }

    public TradeImportPreviewResponse preview(User user, Long accountId, MultipartFile file) {
        ParsedImport parsed = parse(user, accountId, file);
        return toPreview(user, accountId, parsed);
    }

    public TradeImportSaveResponse save(User user, Long accountId, MultipartFile file) {
        ParsedImport parsed = parse(user, accountId, file);
        TradeImportPreviewResponse preview = toPreview(user, accountId, parsed);
        List<TradeResponse> saved = parsed.trades().isEmpty()
                ? List.of()
                : tradeService.importBrokerTrades(user, parsed.trades());
        tradeReplayService.initializeAfterCommit(tradeService.getImportedTradesForReplayInitialization(user, saved.stream().map(TradeResponse::getId).toList()));
        return new TradeImportSaveResponse(preview, saved, saved.size());
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
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "upload.csv";
        }
        return name.replaceAll("[\\\\/]", "_");
    }
}
