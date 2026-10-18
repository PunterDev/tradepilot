package com.tradepilot.importing;

final class ImportTelemetryException extends IllegalArgumentException {
    private final ImportTelemetry.ParserSource parserSource;
    private final ImportTelemetry.FailureCategory failureCategory;

    ImportTelemetryException(String message, ImportTelemetry.ParserSource parserSource, ImportTelemetry.FailureCategory failureCategory, Throwable cause) {
        super(message, cause);
        this.parserSource = parserSource;
        this.failureCategory = failureCategory;
    }

    ImportTelemetryException(String message, ImportTelemetry.ParserSource parserSource, ImportTelemetry.FailureCategory failureCategory) {
        this(message, parserSource, failureCategory, null);
    }

    ImportTelemetry.ParserSource parserSource() { return parserSource; }
    ImportTelemetry.FailureCategory failureCategory() { return failureCategory; }
}
