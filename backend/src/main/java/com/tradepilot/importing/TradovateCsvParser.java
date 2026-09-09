package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportFormat;
import com.tradepilot.dto.trade.TradeImportRequest;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.tradereplay.service.ContractSymbolNormalizationResult;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.service.FuturesContractSymbolNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class TradovateCsvParser {
    private static final Logger log = LoggerFactory.getLogger(TradovateCsvParser.class);
    private static final BigDecimal MAX_PNL_RECONCILIATION_DELTA = new BigDecimal("25.00");
    private static final Set<String> DASHBOARD_HEADERS = Set.of(
            "symbol",
            "qty",
            "buyPrice",
            "sellPrice",
            "pnl",
            "boughtTimestamp",
            "soldTimestamp"
    );
    private static final Set<String> PERFORMANCE_HEADERS = Set.of(
            "symbol",
            "buyFillId",
            "sellFillId",
            "qty",
            "buyPrice",
            "sellPrice",
            "pnl",
            "boughtTimestamp",
            "soldTimestamp"
    );
    private static final List<DateTimeFormatter> TIMESTAMP_FORMATTERS = List.of(
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy HH:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy H:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy HH:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy H:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm:ss a").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm a").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy h:mm:ss a").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yy h:mm a").toFormatter(Locale.US)
    );
    private final TradingSessionClassifier sessionClassifier;
    private final FuturesContractSymbolNormalizer contractSymbolNormalizer;
    private final FuturesPointValueService pointValueService;

    public TradovateCsvParser(
            TradingSessionClassifier sessionClassifier,
            FuturesContractSymbolNormalizer contractSymbolNormalizer,
            FuturesPointValueService pointValueService
    ) {
        this.sessionClassifier = sessionClassifier;
        this.contractSymbolNormalizer = contractSymbolNormalizer;
        this.pointValueService = pointValueService;
    }

    public boolean supports(CsvTable table) {
        return table.hasHeaders(DASHBOARD_HEADERS) || table.hasHeaders(PERFORMANCE_HEADERS);
    }

    public ParsedImport parse(String csv, Long accountId, String fileName) {
        CsvTable table = CsvTable.parse(csv);
        if (!supports(table)) {
            throw new IllegalArgumentException("Unsupported Tradovate CSV headers.");
        }
        List<String> warnings = new ArrayList<>();
        List<TradeImportRequest> requests = new ArrayList<>();
        int invalidRows = 0;

        for (int index = 0; index < table.rows().size(); index++) {
            Map<String, String> row = table.rows().get(index);
            int rowNumber = index + 2;
            boolean performanceRow = isPerformanceTable(table);
            try {
                requests.add(parseRow(row, accountId, fileName, rowNumber, performanceRow));
            } catch (IllegalArgumentException ex) {
                invalidRows++;
                warnings.add(ex.getMessage());
            }
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("No valid Tradovate trades were found in this CSV.");
        }

        return new ParsedImport(
                TradeImportFormat.TRADOVATE,
                List.of(),
                table.rows().size(),
                requests.size(),
                0,
                invalidRows,
                0,
                warnings,
                requests,
                requests.stream().map(request -> List.<String>of()).toList()
        );
    }

    private boolean isPerformanceTable(CsvTable table) {
        return table.hasHeaders(PERFORMANCE_HEADERS);
    }

    private TradeImportRequest parseRow(Map<String, String> row, Long accountId, String fileName, int rowNumber, boolean performanceRow) {
        String brokerSymbol = required(row, "symbol", rowNumber);
        String boughtTimestamp = required(row, "boughtTimestamp", rowNumber);
        String soldTimestamp = required(row, "soldTimestamp", rowNumber);
        String buyFillId = performanceRow ? required(row, "buyFillId", rowNumber) : null;
        String sellFillId = performanceRow ? required(row, "sellFillId", rowNumber) : null;
        BigDecimal buyPrice = parseDecimal(required(row, "buyPrice", rowNumber), "buyPrice", rowNumber);
        BigDecimal sellPrice = parseDecimal(required(row, "sellPrice", rowNumber), "sellPrice", rowNumber);
        int quantity = parsePositiveInt(required(row, "qty", rowNumber), "qty", rowNumber);
        BigDecimal pnl = parseMoney(required(row, "pnl", rowNumber), rowNumber);
        LocalDateTime boughtAt = parseTimestamp(boughtTimestamp, rowNumber);
        LocalDateTime soldAt = parseTimestamp(soldTimestamp, rowNumber);
        if (performanceRow && boughtAt.equals(soldAt)) {
            throw new IllegalArgumentException("Row " + rowNumber + " has equal boughtTimestamp and soldTimestamp; matched Tradovate Performance rows require distinct entry and exit times.");
        }
        String side = inferSide(row, boughtAt, soldAt);
        if (performanceRow && "UNKNOWN".equals(side)) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid timestamp ordering; unable to infer Tradovate Performance direction.");
        }
        boolean isShort = "SHORT".equals(side);
        LocalDateTime entryTime = isShort ? soldAt : boughtAt;
        LocalDateTime exitTime = isShort ? boughtAt : soldAt;
        BigDecimal entryPrice = isShort ? sellPrice : buyPrice;
        BigDecimal exitPrice = isShort ? buyPrice : sellPrice;
        ContractSymbolNormalizationResult contract = contractSymbolNormalizer.normalize(brokerSymbol, entryTime);
        String rootSymbol = contract.valid() ? contract.rootInstrument() : contractSymbolNormalizer.rootInstrument(brokerSymbol);
        if (rootSymbol == null) {
            rootSymbol = brokerSymbol.toUpperCase(Locale.ROOT);
        }
        reconcilePnl(rowNumber, brokerSymbol, rootSymbol, side, entryPrice, exitPrice, quantity, pnl);
        String externalKey = performanceRow ? performanceExternalKey(
                accountId,
                brokerSymbol,
                buyFillId,
                sellFillId,
                quantity,
                buyPrice,
                sellPrice,
                pnl,
                boughtTimestamp,
                soldTimestamp
        ) : String.join("|",
                String.valueOf(accountId),
                brokerSymbol.toUpperCase(Locale.ROOT),
                boughtTimestamp,
                soldTimestamp,
                String.valueOf(quantity),
                buyPrice.stripTrailingZeros().toPlainString(),
                sellPrice.stripTrailingZeros().toPlainString(),
                pnl.stripTrailingZeros().toPlainString()
        );
        com.tradepilot.enums.SessionType session = classifySession(entryTime, performanceRow, rowNumber, brokerSymbol, boughtTimestamp, soldTimestamp);
        return new TradeImportRequest(
                accountId,
                fileName,
                externalKey,
                rootSymbol,
                "FUTURES",
                exitTime.toLocalDate(),
                entryTime,
                exitTime,
                side,
                SetupType.OTHER,
                session,
                entryPrice,
                exitPrice,
                quantity,
                pnl,
                null,
                pnl.compareTo(BigDecimal.ZERO) > 0 ? OutcomeType.WIN : pnl.compareTo(BigDecimal.ZERO) < 0 ? OutcomeType.LOSS : OutcomeType.BE,
                brokerSymbol,
                contract.valid() ? contract.normalizedContractSymbol() : null,
                contract.valid() ? ContractSymbolSource.IMPORTED.name() : null
        );
    }

    private com.tradepilot.enums.SessionType classifySession(LocalDateTime entryTime, boolean performanceRow, int rowNumber, String brokerSymbol, String boughtTimestamp, String soldTimestamp) {
        if (!performanceRow) {
            com.tradepilot.enums.SessionType session = sessionClassifier.classifyMarketLocalTimestamp(entryTime);
            logSessionClassification(rowNumber, brokerSymbol, "TRADOVATE_DASHBOARD", boughtTimestamp, soldTimestamp, entryTime, TradingSessionPolicy.MARKET_ZONE, session);
            return session;
        }
        com.tradepilot.enums.SessionType session = sessionClassifier.classifySourceLocalTimestamp(entryTime, TradingSessionPolicy.TRADOVATE_BROKER_ZONE);
        logSessionClassification(rowNumber, brokerSymbol, "TRADOVATE_PERFORMANCE", boughtTimestamp, soldTimestamp, entryTime, TradingSessionPolicy.TRADOVATE_BROKER_ZONE, session);
        return session;
    }

    private void logSessionClassification(int rowNumber, String brokerSymbol, String sourceFormat, String boughtTimestamp, String soldTimestamp, LocalDateTime entryTime, java.time.ZoneId sourceZone, com.tradepilot.enums.SessionType session) {
        if (!log.isDebugEnabled() || entryTime == null) {
            return;
        }
        ZonedDateTime sourceDateTime = entryTime.atZone(sourceZone);
        ZonedDateTime marketDateTime = sourceDateTime.toInstant().atZone(TradingSessionPolicy.MARKET_ZONE);
        log.debug(
                "Tradovate session classified. rowNumber={}, symbol={}, sourceFormat={}, rawBoughtTimestamp={}, rawSoldTimestamp={}, sourceZone={}, sourceTimestamp={}, canonicalInstant={}, marketZone={}, marketTimestamp={}, session={}",
                rowNumber,
                brokerSymbol,
                sourceFormat,
                boughtTimestamp,
                soldTimestamp,
                sourceZone,
                entryTime,
                sourceDateTime.toInstant(),
                TradingSessionPolicy.MARKET_ZONE,
                marketDateTime.toLocalDateTime(),
                session
        );
    }

    private String performanceExternalKey(
            Long accountId,
            String brokerSymbol,
            String buyFillId,
            String sellFillId,
            int quantity,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            BigDecimal pnl,
            String boughtTimestamp,
            String soldTimestamp
    ) {
        return String.join("|",
                "TRADOVATE_PERFORMANCE",
                String.valueOf(accountId),
                brokerSymbol.toUpperCase(Locale.ROOT),
                buyFillId,
                sellFillId,
                String.valueOf(quantity),
                buyPrice.stripTrailingZeros().toPlainString(),
                sellPrice.stripTrailingZeros().toPlainString(),
                pnl.stripTrailingZeros().toPlainString(),
                boughtTimestamp,
                soldTimestamp
        );
    }

    private void reconcilePnl(
            int rowNumber,
            String brokerSymbol,
            String rootSymbol,
            String side,
            BigDecimal entryPrice,
            BigDecimal exitPrice,
            int quantity,
            BigDecimal brokerReportedPnl
    ) {
        if (!Set.of("LONG", "SHORT").contains(side)) {
            log.debug("Tradovate pnl reconciliation skipped because side is unresolved. rowNumber={}, symbol={}, side={}", rowNumber, brokerSymbol, side);
            return;
        }
        BigDecimal pointValue = pointValueService.pointValueForContract(rootSymbol)
                .or(() -> pointValueService.pointValueForContract(brokerSymbol))
                .orElse(null);
        if (pointValue == null) {
            log.debug("Tradovate pnl reconciliation skipped because point value is unknown. rowNumber={}, symbol={}, rootSymbol={}", rowNumber, brokerSymbol, rootSymbol);
            return;
        }

        BigDecimal pointMove = "SHORT".equals(side)
                ? entryPrice.subtract(exitPrice)
                : exitPrice.subtract(entryPrice);
        BigDecimal calculatedGrossPnl = pointMove
                .multiply(pointValue)
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedBrokerPnl = brokerReportedPnl.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pnlDelta = normalizedBrokerPnl.subtract(calculatedGrossPnl).setScale(2, RoundingMode.HALF_UP);

        log.info(
                "Tradovate pnl reconciliation completed. rowNumber={}, symbol={}, rootSymbol={}, side={}, quantity={}, entryPrice={}, exitPrice={}, calculatedGrossPnl={}, brokerReportedPnl={}, pnlDelta={}",
                rowNumber,
                brokerSymbol,
                rootSymbol,
                side,
                quantity,
                entryPrice,
                exitPrice,
                calculatedGrossPnl,
                normalizedBrokerPnl,
                pnlDelta
        );

        if (pnlDelta.abs().compareTo(MAX_PNL_RECONCILIATION_DELTA) > 0) {
            log.warn(
                    "Tradovate pnl reconciliation rejected row. rowNumber={}, symbol={}, rootSymbol={}, side={}, quantity={}, entryPrice={}, exitPrice={}, calculatedGrossPnl={}, brokerReportedPnl={}, pnlDelta={}, tolerance={}",
                    rowNumber,
                    brokerSymbol,
                    rootSymbol,
                    side,
                    quantity,
                    entryPrice,
                    exitPrice,
                    calculatedGrossPnl,
                    normalizedBrokerPnl,
                    pnlDelta,
                    MAX_PNL_RECONCILIATION_DELTA
            );
            throw new IllegalArgumentException("Row " + rowNumber
                    + " has inconsistent Tradovate pnl: calculatedGrossPnl=" + calculatedGrossPnl.toPlainString()
                    + ", brokerReportedPnl=" + normalizedBrokerPnl.toPlainString()
                    + ", pnlDelta=" + pnlDelta.toPlainString());
        }
    }

    private String inferSide(Map<String, String> row, LocalDateTime boughtAt, LocalDateTime soldAt) {
        String explicit = firstNonBlank(
                CsvTable.get(row, "side"),
                CsvTable.get(row, "direction"),
                CsvTable.get(row, "position"),
                CsvTable.get(row, "openingAction"),
                CsvTable.get(row, "openAction"),
                CsvTable.get(row, "entryAction"),
                CsvTable.get(row, "action")
        );
        String explicitSide = sideFromText(explicit);
        if (explicitSide != null) {
            return explicitSide;
        }
        if (boughtAt.isBefore(soldAt)) {
            return "LONG";
        }
        if (soldAt.isBefore(boughtAt)) {
            return "SHORT";
        }
        return "UNKNOWN";
    }

    private String sideFromText(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim().toUpperCase(Locale.ROOT);
        if (Set.of("LONG", "BUY", "BOUGHT", "BOT", "B").contains(clean)) {
            return "LONG";
        }
        if (Set.of("SHORT", "SELL", "SOLD", "SLD", "S").contains(clean)) {
            return "SHORT";
        }
        return "UNKNOWN".equals(clean) ? "UNKNOWN" : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String clean = clean(value);
            if (clean != null) {
                return clean;
            }
        }
        return null;
    }

    private String required(Map<String, String> row, String header, int rowNumber) {
        String value = clean(CsvTable.get(row, header));
        if (value == null) {
            throw new IllegalArgumentException("Row " + rowNumber + " is missing required value: " + header);
        }
        return value;
    }

    private BigDecimal parseDecimal(String value, String field, int rowNumber) {
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid " + field + ": " + value);
        }
    }

    private BigDecimal parseMoney(String value, int rowNumber) {
        String clean = value.replace("$", "").replace(",", "").trim();
        if (clean.startsWith("(") && clean.endsWith(")")) {
            clean = "-" + clean.substring(1, clean.length() - 1);
        }
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid pnl: " + value);
        }
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

    private LocalDateTime parseTimestamp(String value, int rowNumber) {
        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Row " + rowNumber + " has invalid timestamp: " + value);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }
}
