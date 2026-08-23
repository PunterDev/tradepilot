package com.tradepilot.tradereplay.dto;

import java.util.List;

public record ContractCandidatesResponse(
        Long tradeId,
        String rootInstrument,
        String originalBrokerSymbol,
        List<ContractCandidateResponse> candidates
) {}
