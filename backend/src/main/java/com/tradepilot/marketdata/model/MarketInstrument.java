package com.tradepilot.marketdata.model;

import java.math.BigDecimal;
import java.util.Locale;

public enum MarketInstrument {
    NQ("NQ.v.0", new BigDecimal("0.25")),
    ES("ES.v.0", new BigDecimal("0.25"));

    private final String continuousSymbol;
    private final BigDecimal tickSize;

    MarketInstrument(String continuousSymbol, BigDecimal tickSize) {
        this.continuousSymbol = continuousSymbol;
        this.tickSize = tickSize;
    }

    public String continuousSymbol() {
        return continuousSymbol;
    }

    public BigDecimal tickSize() {
        return tickSize;
    }

    public static MarketInstrument from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Instrument must be NQ or ES.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Instrument must be NQ or ES.");
        }
    }
}
