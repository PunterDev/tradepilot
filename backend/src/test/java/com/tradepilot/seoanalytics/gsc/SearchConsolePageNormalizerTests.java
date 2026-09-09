package com.tradepilot.seoanalytics.gsc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchConsolePageNormalizerTests {
    private final GoogleSearchConsoleProperties properties = new GoogleSearchConsoleProperties();
    private final SearchConsolePageNormalizer normalizer = new SearchConsolePageNormalizer(properties);

    @Test
    void normalizesCanonicalWwwTrailingSlashQueryAndHomepageUrls() {
        assertThat(normalizer.normalize("https://tradepilotcoach.com/revenge-trading")).contains("/revenge-trading");
        assertThat(normalizer.normalize("https://www.tradepilotcoach.com/revenge-trading")).contains("/revenge-trading");
        assertThat(normalizer.normalize("https://tradepilotcoach.com/revenge-trading/")).contains("/revenge-trading");
        assertThat(normalizer.normalize("https://tradepilotcoach.com/revenge-trading?utm_source=x#top")).contains("/revenge-trading");
        assertThat(normalizer.normalize("https://tradepilotcoach.com/")).contains("/");
    }

    @Test
    void ignoresExternalUrls() {
        assertThat(normalizer.normalize("https://example.com/revenge-trading")).isEmpty();
    }
}
