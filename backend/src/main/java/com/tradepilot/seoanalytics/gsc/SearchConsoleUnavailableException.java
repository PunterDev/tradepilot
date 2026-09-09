package com.tradepilot.seoanalytics.gsc;

class SearchConsoleUnavailableException extends RuntimeException {
    SearchConsoleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    SearchConsoleUnavailableException(String message) {
        super(message);
    }
}
