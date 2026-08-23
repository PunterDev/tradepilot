package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportFormat;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.tradereplay.service.ContractSymbolNormalizationResult;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class TopstepCsvParser {
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "Id",
            "AccountName",
            "ContractName",
            "Status",
            "Size",
            "Side",
            "FilledAt",
            "ExecutePrice",
            "PositionDisposition",
            "CreationDisposition"
    );
    private static final DateTimeFormatter TOPSTEP_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MM/dd/yyyy HH:mm:ss XXX")
            .toFormatter(Locale.US);

    private final FuturesPointValueService pointValueService;
    private final TradingSessionClassifier sessionClassifier;
    private final FuturesContractSymbolNormalizer contractSymbolNormalizer;

    public TopstepCsvParser(FuturesPointValueService pointValueService, TradingSessionClassifier sessionClassifier, FuturesContractSymbolNormalizer contractSymbolNormalizer) {
        this.pointValueService = pointValueService;
        this.sessionClassifier = sessionClassifier;
        this.contractSymbolNormalizer = contractSymbolNormalizer;
    }

    public boolean supports(CsvTable table) {
        return table.hasHeaders(REQUIRED_HEADERS);
    }

    public ParsedImport parse(String csv, Long accountId, String fileName) {
        CsvTable table = CsvTable.parse(csv);
        if (!supports(table)) {
            throw new IllegalArgumentException("Unsupported CSV format. Upload a supported Tradovate or Topstep CSV export.");
        }

        List<String> warnings = new ArrayList<>();
        List<Execution> executions = new ArrayList<>();
        Set<String> sourceAccounts = new LinkedHashSet<>();
        int totalRows = table.rows().size();
        int filledRows = 0;
        int cancelledRowsIgnored = 0;
        int invalidRows = 0;

        for (int index = 0; index < table.rows().size(); index++) {
            Map<String, String> row = table.rows().get(index);
            int rowNumber = index + 2;
            String status = clean(CsvTable.get(row, "Status"));
            String accountName = clean(CsvTable.get(row, "AccountName"));
            if (accountName != null) {
                sourceAccounts.add(accountName);
            }

            if ("Cancelled".equalsIgnoreCase(status)) {
                cancelledRowsIgnored++;
                continue;
            }
            if (!"Filled".equalsIgnoreCase(status)) {
                invalidRows++;
                warnings.add("Row " + rowNumber + " ignored because status is unsupported: " + safe(status));
                continue;
            }
            filledRows++;

            String filledAtText = clean(CsvTable.get(row, "FilledAt"));
            String executePriceText = clean(CsvTable.get(row, "ExecutePrice"));
            String disposition = clean(CsvTable.get(row, "PositionDisposition"));
            if (filledAtText == null || executePriceText == null || disposition == null || "Undetermined".equalsIgnoreCase(disposition)) {
                invalidRows++;
                warnings.add("Row " + rowNumber + " ignored because it is not a completed opening/closing fill.");
                continue;
            }

            try {
                Execution execution = parseExecution(row, rowNumber);
                executions.add(execution);
            } catch (IllegalArgumentException ex) {
                invalidRows++;
                warnings.add(ex.getMessage());
            }
        }

        if (sourceAccounts.size() > 1) {
            throw new IllegalArgumentException("Topstep import currently supports one source AccountName per file. Found: " + sourceAccounts.size());
        }
        if (executions.isEmpty()) {
            throw new IllegalArgumentException("No filled Topstep executions were found in this CSV.");
        }

        RebuildResult rebuild = rebuildTrades(executions, accountId, fileName, sourceAccounts.stream().findFirst().orElse("UNKNOWN"), warnings);
        return new ParsedImport(
                TradeImportFormat.TOPSTEP,
                List.copyOf(sourceAccounts),
                totalRows,
                filledRows,
                cancelledRowsIgnored,
                invalidRows + rebuild.invalidTrades(),
                rebuild.incompletePositions(),
                List.copyOf(warnings),
                rebuild.requests(),
                rebuild.sourceExecutionIds()
        );
    }

    private Execution parseExecution(Map<String, String> row, int rowNumber) {
        String id = required(row, "Id", rowNumber);
        String sourceAccount = required(row, "AccountName", rowNumber);
        String originalContract = required(row, "ContractName", rowNumber);
        int size = parsePositiveInt(required(row, "Size", rowNumber), "Size", rowNumber);
        ExecutionSide side = parseSide(required(row, "Side", rowNumber), rowNumber);
        PositionDisposition disposition = parseDisposition(required(row, "PositionDisposition", rowNumber), rowNumber);
        OffsetDateTime filledAt = parseTimestamp(required(row, "FilledAt", rowNumber), "FilledAt", rowNumber);
        OffsetDateTime tradeDayTimestamp = parseOptionalTimestamp(CsvTable.get(row, "TradeDay"), "TradeDay", rowNumber);
        BigDecimal price = parsePositiveDecimal(required(row, "ExecutePrice", rowNumber), "ExecutePrice", rowNumber);
        ContractSymbolNormalizationResult contract = contractSymbolNormalizer.normalize(originalContract, filledAt.toLocalDateTime());
        String groupingContract = contract.valid() ? contract.normalizedContractSymbol() : originalContract.toUpperCase(Locale.ROOT);
        String rootSymbol = contract.valid() ? contract.rootInstrument() : contractSymbolNormalizer.rootInstrument(originalContract);
        return new Execution(id, sourceAccount, groupingContract, originalContract, rootSymbol, contract.valid() ? contract.normalizedContractSymbol() : null, size, side, disposition, filledAt, tradeDayTimestamp, price);
    }

    private RebuildResult rebuildTrades(List<Execution> executions, Long accountId, String fileName, String sourceAccount, List<String> warnings) {
        List<TradeImportRequest> requests = new ArrayList<>();
        List<List<String>> sourceExecutionIds = new ArrayList<>();
        int incompletePositions = 0;
        int invalidTrades = 0;

        Map<String, List<Execution>> byContract = executions.stream()
                .sorted(Comparator.comparing(Execution::filledAt).thenComparing(Execution::id))
                .collect(java.util.stream.Collectors.groupingBy(Execution::contract, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

        for (Map.Entry<String, List<Execution>> entry : byContract.entrySet()) {
            String contract = entry.getKey();
            BigDecimal pointValue = pointValueService.pointValueForContract(contract)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported futures contract for Topstep import: " + contract));
            OpenPosition position = null;
            for (Execution execution : entry.getValue()) {
                int signedQuantity = execution.side() == ExecutionSide.BUY ? execution.size() : -execution.size();
                if (execution.disposition() == PositionDisposition.OPENING) {
                    if (position == null || position.isFlat()) {
                        position = OpenPosition.open(execution, signedQuantity);
                    } else if (Integer.signum(position.signedQuantity) == Integer.signum(signedQuantity)) {
                        position.add(execution, signedQuantity);
                    } else {
                        warnings.add("Execution " + execution.id() + " was marked Opening against an existing opposite position and was skipped.");
                        invalidTrades++;
                    }
                    continue;
                }

                if (position == null || position.isFlat()) {
                    warnings.add("Execution " + execution.id() + " was marked Closing without an open position and was skipped.");
                    invalidTrades++;
                    continue;
                }

                int remainingToClose = execution.size();
                while (remainingToClose > 0 && position != null && !position.isFlat()) {
                    int closeSize = Math.min(Math.abs(position.signedQuantity), remainingToClose);
                    position.close(execution, closeSize, pointValue);
                    remainingToClose -= closeSize;
                    if (position.isFlat()) {
                        CompletedTrade completed = position.toCompletedTrade(contract, accountId, fileName, sourceAccount, sessionClassifier);
                        requests.add(completed.request());
                        sourceExecutionIds.add(completed.sourceExecutionIds());
                        position = null;
                    }
                }

                if (remainingToClose > 0) {
                    position = OpenPosition.open(execution, execution.side() == ExecutionSide.BUY ? remainingToClose : -remainingToClose);
                }
            }
            if (position != null && !position.isFlat()) {
                incompletePositions++;
                warnings.add("Open Topstep position in " + contract + " was not imported because it never returned flat.");
            }
        }

        return new RebuildResult(requests, sourceExecutionIds, incompletePositions, invalidTrades);
    }

    private String required(Map<String, String> row, String header, int rowNumber) {
        String value = clean(CsvTable.get(row, header));
        if (value == null) {
            throw new IllegalArgumentException("Row " + rowNumber + " is missing required value: " + header);
        }
        return value;
    }

    private int parsePositiveInt(String value, String field, int rowNumber) {
        try {
            int parsed = Integer.parseInt(value.replace(",", ""));
            if (parsed <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid " + field + ": " + value);
        }
    }

    private BigDecimal parsePositiveDecimal(String value, String field, int rowNumber) {
        try {
            BigDecimal parsed = new BigDecimal(value.replace(",", ""));
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid " + field + ": " + value);
        }
    }

    private OffsetDateTime parseTimestamp(String value, String field, int rowNumber) {
        try {
            return OffsetDateTime.parse(value, TOPSTEP_TIMESTAMP_FORMATTER);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid " + field + " timestamp: " + value);
        }
    }

    private OffsetDateTime parseOptionalTimestamp(String value, String field, int rowNumber) {
        String clean = clean(value);
        return clean == null ? null : parseTimestamp(clean, field, rowNumber);
    }

    private ExecutionSide parseSide(String value, int rowNumber) {
        if ("Bid".equalsIgnoreCase(value)) {
            return ExecutionSide.BUY;
        }
        if ("Ask".equalsIgnoreCase(value)) {
            return ExecutionSide.SELL;
        }
        throw new IllegalArgumentException("Row " + rowNumber + " has unsupported Side: " + value);
    }

    private PositionDisposition parseDisposition(String value, int rowNumber) {
        if ("Opening".equalsIgnoreCase(value)) {
            return PositionDisposition.OPENING;
        }
        if ("Closing".equalsIgnoreCase(value)) {
            return PositionDisposition.CLOSING;
        }
        throw new IllegalArgumentException("Row " + rowNumber + " has unsupported PositionDisposition: " + value);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private String safe(String value) {
        return value == null ? "blank" : value;
    }

    private enum ExecutionSide {
        BUY,
        SELL
    }

    private enum PositionDisposition {
        OPENING,
        CLOSING
    }

    private record Execution(
            String id,
            String sourceAccount,
            String contract,
            String originalContract,
            String rootSymbol,
            String contractSymbol,
            int size,
            ExecutionSide side,
            PositionDisposition disposition,
            OffsetDateTime filledAt,
            OffsetDateTime tradeDayTimestamp,
            BigDecimal price
    ) {
    }

    private record CompletedTrade(TradeImportRequest request, List<String> sourceExecutionIds) {
    }

    private record RebuildResult(
            List<TradeImportRequest> requests,
            List<List<String>> sourceExecutionIds,
            int incompletePositions,
            int invalidTrades
    ) {
    }

    private static final class OpenPosition {
        private int signedQuantity;
        private final int openingSign;
        private int totalEntryQuantity;
        private int totalClosedQuantity;
        private BigDecimal averageEntryPrice;
        private BigDecimal weightedExitPrice = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;
        private final OffsetDateTime entryTime;
        private OffsetDateTime exitTime;
        private final LocalDate tradeDate;
        private final List<String> executionIds = new ArrayList<>();
        private final List<String> originalContracts = new ArrayList<>();
        private final List<String> executionRoots = new ArrayList<>();
        private final List<String> executionContracts = new ArrayList<>();

        private OpenPosition(Execution execution, int signedQuantity) {
            this.signedQuantity = signedQuantity;
            this.openingSign = Integer.signum(signedQuantity);
            this.totalEntryQuantity = Math.abs(signedQuantity);
            this.averageEntryPrice = execution.price();
            this.entryTime = execution.filledAt();
            this.tradeDate = execution.tradeDayTimestamp() == null
                    ? execution.filledAt().toLocalDate()
                    : execution.tradeDayTimestamp().toLocalDate();
            this.executionIds.add(execution.id());
            recordContractFields(execution);
        }

        static OpenPosition open(Execution execution, int signedQuantity) {
            return new OpenPosition(execution, signedQuantity);
        }

        boolean isFlat() {
            return signedQuantity == 0;
        }

        void add(Execution execution, int signedQuantityToAdd) {
            int additionalQuantity = Math.abs(signedQuantityToAdd);
            int currentOpenQuantity = Math.abs(signedQuantity);
            BigDecimal currentNotional = averageEntryPrice.multiply(BigDecimal.valueOf(currentOpenQuantity));
            BigDecimal additionalNotional = execution.price().multiply(BigDecimal.valueOf(additionalQuantity));
            totalEntryQuantity += additionalQuantity;
            signedQuantity += signedQuantityToAdd;
            averageEntryPrice = currentNotional.add(additionalNotional)
                    .divide(BigDecimal.valueOf(currentOpenQuantity + additionalQuantity), 6, RoundingMode.HALF_UP);
            executionIds.add(execution.id());
            recordContractFields(execution);
        }

        void close(Execution execution, int closeSize, BigDecimal pointValue) {
            int positionSign = Integer.signum(signedQuantity);
            BigDecimal priceDelta = positionSign > 0
                    ? execution.price().subtract(averageEntryPrice)
                    : averageEntryPrice.subtract(execution.price());
            BigDecimal closePnl = priceDelta.multiply(BigDecimal.valueOf(closeSize)).multiply(pointValue);
            realizedPnl = realizedPnl.add(closePnl);
            weightedExitPrice = weightedExitPrice.add(execution.price().multiply(BigDecimal.valueOf(closeSize)));
            totalClosedQuantity += closeSize;
            signedQuantity -= positionSign * closeSize;
            exitTime = execution.filledAt();
            executionIds.add(execution.id());
            recordContractFields(execution);
        }

        private void recordContractFields(Execution execution) {
            originalContracts.add(execution.originalContract());
            executionRoots.add(execution.rootSymbol());
            executionContracts.add(execution.contractSymbol());
        }

        CompletedTrade toCompletedTrade(String contract, Long accountId, String fileName, String sourceAccount, TradingSessionClassifier sessionClassifier) {
            BigDecimal exitPrice = totalClosedQuantity == 0
                    ? averageEntryPrice
                    : weightedExitPrice.divide(BigDecimal.valueOf(totalClosedQuantity), 6, RoundingMode.HALF_UP);
            BigDecimal roundedPnl = realizedPnl.setScale(2, RoundingMode.HALF_UP);
            String direction = totalEntryQuantity == 0 ? "UNKNOWN" : direction();
            String externalKey = String.join("|",
                    "TOPSTEP",
                    String.valueOf(accountId),
                    sourceAccount,
                    contract,
                    direction,
                    entryTime.toString(),
                    exitTime == null ? "" : exitTime.toString(),
                    String.valueOf(totalClosedQuantity),
                    String.join(",", executionIds)
            );
            String rootSymbol = firstRootSymbol();
            String contractSymbol = firstContractSymbol();
            String originalContract = firstOriginalContract(contract);
            TradeImportRequest request = new TradeImportRequest(
                    accountId,
                    fileName,
                    externalKey,
                    rootSymbol == null ? contract : rootSymbol,
                    "FUTURES",
                    tradeDate,
                    toLocalDateTime(entryTime),
                    exitTime == null ? toLocalDateTime(entryTime) : toLocalDateTime(exitTime),
                    direction,
                    SetupType.OTHER,
                    sessionClassifier.classify(entryTime),
                    averageEntryPrice,
                    exitPrice,
                    totalClosedQuantity,
                    roundedPnl,
                    null,
                    roundedPnl.compareTo(BigDecimal.ZERO) > 0 ? OutcomeType.WIN : roundedPnl.compareTo(BigDecimal.ZERO) < 0 ? OutcomeType.LOSS : OutcomeType.BE,
                    originalContract,
                    contractSymbol,
                    contractSymbol == null ? null : ContractSymbolSource.IMPORTED.name()
            );
            return new CompletedTrade(request, List.copyOf(executionIds));
        }

        private String firstRootSymbol() {
            return executionRoots.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
        }

        private String firstContractSymbol() {
            return executionContracts.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
        }

        private String firstOriginalContract(String fallback) {
            return originalContracts.stream().filter(java.util.Objects::nonNull).findFirst().orElse(fallback);
        }

        private String direction() {
            return openingSign > 0 ? "LONG" : "SHORT";
        }

        private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
            return value.toLocalDateTime();
        }
    }
}
