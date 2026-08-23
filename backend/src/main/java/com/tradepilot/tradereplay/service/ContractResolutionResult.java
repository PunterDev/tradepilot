package com.tradepilot.tradereplay.service;

public record ContractResolutionResult(
        boolean resolved,
        String canonicalInstrument,
        String originalBrokerSymbol,
        String providerSymbol,
        String requestedContract,
        String expectedContract,
        boolean storedContractValidForDate,
        String resolutionSource,
        String resolutionReason,
        String failureCode,
        String message
) {
    public static ContractResolutionResult ok(String canonicalInstrument, String originalBrokerSymbol, String providerSymbol) {
        return ok(canonicalInstrument, originalBrokerSymbol, providerSymbol, providerSymbol, providerSymbol, true, null, "EXACT_CONTRACT");
    }

    public static ContractResolutionResult ok(
            String canonicalInstrument,
            String originalBrokerSymbol,
            String providerSymbol,
            String requestedContract,
            String expectedContract,
            boolean storedContractValidForDate,
            String resolutionSource,
            String resolutionReason
    ) {
        return new ContractResolutionResult(
                true,
                canonicalInstrument,
                originalBrokerSymbol,
                providerSymbol,
                requestedContract,
                expectedContract,
                storedContractValidForDate,
                resolutionSource,
                resolutionReason,
                null,
                null
        );
    }

    public static ContractResolutionResult failed(String originalBrokerSymbol, String failureCode, String message) {
        return new ContractResolutionResult(false, null, originalBrokerSymbol, null, null, null, false, null, null, failureCode, message);
    }
}
