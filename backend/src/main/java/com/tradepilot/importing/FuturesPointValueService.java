package com.tradepilot.importing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class FuturesPointValueService {
    private static final Map<String, BigDecimal> POINT_VALUES = Map.ofEntries(
            Map.entry("ES", new BigDecimal("50")),
            Map.entry("MES", new BigDecimal("5")),
            Map.entry("NQ", new BigDecimal("20")),
            Map.entry("MNQ", new BigDecimal("2")),
            Map.entry("YM", new BigDecimal("5")),
            Map.entry("MYM", new BigDecimal("0.5")),
            Map.entry("RTY", new BigDecimal("50")),
            Map.entry("M2K", new BigDecimal("5")),
            Map.entry("CL", new BigDecimal("1000")),
            Map.entry("MCL", new BigDecimal("100")),
            Map.entry("GC", new BigDecimal("100")),
            Map.entry("MGC", new BigDecimal("10")),
            Map.entry("SI", new BigDecimal("5000")),
            Map.entry("SIL", new BigDecimal("1000"))
    );

    public Optional<BigDecimal> pointValueForContract(String contractName) {
        String root = rootSymbol(contractName);
        if (root == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(POINT_VALUES.get(root));
    }

    public String rootSymbol(String contractName) {
        if (contractName == null || contractName.isBlank()) {
            return null;
        }
        String clean = contractName.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < clean.length(); i++) {
            char ch = clean.charAt(i);
            if (isFuturesMonthCode(ch) && i + 1 < clean.length() && Character.isDigit(clean.charAt(i + 1))) {
                return clean.substring(0, i);
            }
        }
        return clean.replaceAll("[^A-Z]", "");
    }

    private boolean isFuturesMonthCode(char ch) {
        return "FGHJKMNQUVXZ".indexOf(ch) >= 0;
    }
}
