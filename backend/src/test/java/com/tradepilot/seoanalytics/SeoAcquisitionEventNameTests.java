package com.tradepilot.seoanalytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeoAcquisitionEventNameTests {

    @Test
    void allowsEveryPublicSampleDemoEventAndRejectsUnknownEvents() {
        assertThat(SeoAcquisitionEventName.isAllowed("sample_demo_opened")).isTrue();
        assertThat(SeoAcquisitionEventName.isAllowed("sample_demo_leak_detector_viewed")).isTrue();
        assertThat(SeoAcquisitionEventName.isAllowed("sample_demo_completed")).isTrue();
        assertThat(SeoAcquisitionEventName.isAllowed("sample_demo_registration_started")).isTrue();
        assertThat(SeoAcquisitionEventName.isAllowed("sample_demo_unknown")).isFalse();
    }
}
