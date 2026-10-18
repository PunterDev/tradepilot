package com.tradepilot.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Safe, operational metadata for persisted CSV import lifecycle events. */
final class ImportTelemetry {
    enum ParserSource { TOPSTEP, TRADOVATE, UNKNOWN }
    enum FailureCategory { ACCOUNT_VALIDATION, FILE_REQUIRED, FILE_TOO_LARGE, FILE_READ_FAILED, AMBIGUOUS_FORMAT, UNSUPPORTED_FORMAT, PARSER_ERROR, SAVE_ERROR, UNKNOWN }

    private ImportTelemetry() { }

    static String failure(ObjectMapper mapper, String stage, ParserSource source, FailureCategory category) {
        return json(mapper, Map.of("stage", stage, "parserSource", source.name(), "failureCategory", category.name()));
    }

    static String completed(ObjectMapper mapper, ParsedImport parsed, int savedRows, int duplicateRows) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("stage", "save");
        values.put("parserSource", sourceFor(parsed).name());
        values.put("totalRows", parsed.totalRows());
        values.put("parsedTrades", parsed.trades().size());
        values.put("savedRows", savedRows);
        values.put("duplicateRows", duplicateRows);
        return json(mapper, values);
    }

    static String firstImport(ObjectMapper mapper, ParsedImport parsed, int savedRows) {
        return json(mapper, Map.of("stage", "save", "parserSource", sourceFor(parsed).name(), "savedRows", savedRows));
    }

    static ParserSource sourceFor(ParsedImport parsed) {
        return parsed.format() == com.tradepilot.dto.trade.TradeImportFormat.TOPSTEP ? ParserSource.TOPSTEP : ParserSource.TRADOVATE;
    }

    private static String json(ObjectMapper mapper, Map<String, ?> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            // Telemetry is best effort; this fallback contains only fixed safe values.
            return "{\"stage\":\"unknown\",\"parserSource\":\"UNKNOWN\",\"failureCategory\":\"UNKNOWN\"}";
        }
    }
}
