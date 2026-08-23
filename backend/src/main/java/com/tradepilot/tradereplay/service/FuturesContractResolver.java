package com.tradepilot.tradereplay.service;

import com.tradepilot.entity.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class FuturesContractResolver {
    private static final Logger log = LoggerFactory.getLogger(FuturesContractResolver.class);
    private static final Set<String> AUTHORITATIVE_EXACT_SOURCES = Set.of(
            ContractSymbolSource.IMPORTED.name(),
            ContractSymbolSource.USER_CONFIRMED.name()
    );
    private static final Set<String> DATE_VALIDATED_EXACT_SOURCES = Set.of(
            ContractSymbolSource.NORMALIZED.name(),
            ContractSymbolSource.ADMIN_BACKFILL.name()
    );
    private final FuturesContractSymbolNormalizer normalizer;
    private final FuturesContractCalendar calendar;

    public FuturesContractResolver(FuturesContractSymbolNormalizer normalizer, FuturesContractCalendar calendar) {
        this.normalizer = normalizer;
        this.calendar = calendar;
    }

    public ContractResolutionResult resolve(Trade trade) {
        if (trade == null) {
            return ContractResolutionResult.failed(null, "CONTRACT_SYMBOL_MISSING", "Trade symbol is missing.");
        }
        ContractResolutionResult exact = resolveSymbol(
                trade.getContractSymbol(),
                trade,
                brokerSymbol(trade),
                cleanSource(trade.getContractSymbolSource()),
                "contractSymbol"
        );
        if (exact.resolved()) {
            return exact;
        }
        ContractResolutionResult broker = resolveSymbol(
                trade.getOriginalBrokerSymbol(),
                trade,
                brokerSymbol(trade),
                "ORIGINAL_BROKER_SYMBOL",
                "originalBrokerSymbol"
        );
        if (broker.resolved()) {
            return broker;
        }
        return resolveSymbol(trade.getSymbol(), trade, brokerSymbol(trade), "ROOT_SYMBOL", "symbol");
    }

    private ContractResolutionResult resolveSymbol(String symbol, Trade trade, String originalBrokerSymbol, String source, String fieldName) {
        LocalDateTime executionTime = trade.getEntryTime();
        ContractSymbolNormalizationResult result = normalizer.normalize(symbol, executionTime);
        if (!result.valid()) {
            return ContractResolutionResult.failed(originalBrokerSymbol, result.failureCode(), result.message());
        }
        String tradeRoot = normalizer.rootInstrument(trade.getSymbol());
        if (tradeRoot != null && !tradeRoot.equals(result.rootInstrument())) {
            return ContractResolutionResult.failed(originalBrokerSymbol, "UNSUPPORTED_INSTRUMENT", "Confirmed contract does not match the trade instrument.");
        }
        LocalDate tradeDate = trade.getTradeDate() == null && executionTime != null ? executionTime.toLocalDate() : trade.getTradeDate();
        Optional<String> expected = calendar.expectedActiveContract(result.rootInstrument(), tradeDate);
        boolean validForDate = calendar.contractValidForDate(result.normalizedContractSymbol(), tradeDate, executionTime);
        boolean exactMatchesExpected = expected.map(result.normalizedContractSymbol()::equals).orElse(true);
        String resolutionReason = resolutionReason(source, trade, exactMatchesExpected);
        String providerSymbol = result.normalizedContractSymbol();

        if (shouldUseExpectedContract(source, trade, fieldName, expected, exactMatchesExpected)) {
            providerSymbol = expected.get();
            resolutionReason = "DATE_VALIDATED_FALLBACK";
        }

        log.info(
                "Trade Replay contract validation. tradeId={}, root={}, tradeDate={}, storedContract={}, contractSource={}, expectedContract={}, storedContractValidForDate={}, resolvedProviderSymbol={}, resolutionAction={}",
                trade.getId(),
                result.rootInstrument(),
                tradeDate,
                result.normalizedContractSymbol(),
                source,
                expected.orElse(null),
                validForDate,
                providerSymbol,
                resolutionReason
        );

        return ContractResolutionResult.ok(
                result.rootInstrument(),
                originalBrokerSymbol,
                providerSymbol,
                result.normalizedContractSymbol(),
                expected.orElse(null),
                validForDate,
                source,
                resolutionReason
        );
    }

    private boolean shouldUseExpectedContract(String source, Trade trade, String fieldName, Optional<String> expected, boolean exactMatchesExpected) {
        if (expected.isEmpty() || exactMatchesExpected) {
            return false;
        }
        if (!"contractSymbol".equals(fieldName)) {
            return false;
        }
        if (isDemoTrade(trade)) {
            return true;
        }
        return source == null || DATE_VALIDATED_EXACT_SOURCES.contains(source);
    }

    private String resolutionReason(String source, Trade trade, boolean exactMatchesExpected) {
        if (!exactMatchesExpected && isDemoTrade(trade)) {
            return "DATE_VALIDATED_FALLBACK";
        }
        if ("ORIGINAL_BROKER_SYMBOL".equals(source) || AUTHORITATIVE_EXACT_SOURCES.contains(source)) {
            return "TRUST_EXACT_CONTRACT";
        }
        if (source == null) {
            return "LEGACY_EXACT_CONTRACT";
        }
        return "DATE_VALIDATED_EXACT_CONTRACT";
    }

    private boolean isDemoTrade(Trade trade) {
        String key = trade.getExternalTradeKey();
        return key != null && key.toUpperCase(Locale.ROOT).startsWith("DEV-DEMO-");
    }

    private String cleanSource(String source) {
        return source == null || source.isBlank() ? null : source.trim().toUpperCase(Locale.ROOT);
    }

    private String brokerSymbol(Trade trade) {
        if (trade.getOriginalBrokerSymbol() != null && !trade.getOriginalBrokerSymbol().isBlank()) return trade.getOriginalBrokerSymbol();
        if (trade.getContractSymbol() != null && !trade.getContractSymbol().isBlank()) return trade.getContractSymbol();
        return trade.getSymbol();
    }
}
