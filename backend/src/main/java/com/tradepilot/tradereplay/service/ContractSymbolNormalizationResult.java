package com.tradepilot.tradereplay.service;

public record ContractSymbolNormalizationResult(
        boolean valid,
        String rootInstrument,
        String originalSymbol,
        String normalizedContractSymbol,
        String failureCode,
        String message
) {
    static ContractSymbolNormalizationResult valid(String rootInstrument, String originalSymbol, String normalizedContractSymbol) {
        return new ContractSymbolNormalizationResult(true, rootInstrument, originalSymbol, normalizedContractSymbol, null, null);
    }

    static ContractSymbolNormalizationResult failed(String originalSymbol, String failureCode, String message) {
        return new ContractSymbolNormalizationResult(false, null, originalSymbol, null, failureCode, message);
    }
}
