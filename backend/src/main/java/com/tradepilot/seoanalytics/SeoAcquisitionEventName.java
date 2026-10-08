package com.tradepilot.seoanalytics;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum SeoAcquisitionEventName {
    SEO_LANDING_PAGE_VIEW("seo_landing_page_view"),
    SEO_CTA_CLICK("seo_cta_click"),
    SIGNUP_STARTED_FROM_SEO("signup_started_from_seo"),
    SAMPLE_DEMO_OPENED("sample_demo_opened"),
    SAMPLE_DEMO_LEAK_DETECTOR_VIEWED("sample_demo_leak_detector_viewed"),
    SAMPLE_DEMO_COMPLETED("sample_demo_completed"),
    SAMPLE_DEMO_REGISTRATION_STARTED("sample_demo_registration_started"),
    TOOL_PAGE_VIEW("tool_page_view"),
    EXPECTANCY_CALCULATOR_STARTED("expectancy_calculator_started"),
    EXPECTANCY_CALCULATOR_COMPLETED("expectancy_calculator_completed"),
    EXPECTANCY_RESULT_POSITIVE("expectancy_result_positive"),
    EXPECTANCY_RESULT_NEGATIVE("expectancy_result_negative"),
    EXPECTANCY_RESULT_BREAKEVEN("expectancy_result_breakeven"),
    EXPECTANCY_CTA_CLICK("expectancy_cta_click"),
    OVERTRADING_CALCULATOR_STARTED("overtrading_calculator_started"),
    OVERTRADING_CALCULATOR_COMPLETED("overtrading_calculator_completed"),
    OVERTRADING_RESULT_LATER_WORSE("overtrading_result_later_worse"),
    OVERTRADING_RESULT_SIMILAR("overtrading_result_similar"),
    OVERTRADING_RESULT_LATER_BETTER("overtrading_result_later_better"),
    OVERTRADING_CTA_CLICK("overtrading_cta_click"),
    REVENGE_CALCULATOR_STARTED("revenge_calculator_started"),
    REVENGE_CALCULATOR_COMPLETED("revenge_calculator_completed"),
    REVENGE_RESULT_NO_CLEAR_DETERIORATION("revenge_result_no_clear_deterioration"),
    REVENGE_RESULT_MILD_DETERIORATION("revenge_result_mild_deterioration"),
    REVENGE_RESULT_STRONG_POST_LOSS_DETERIORATION("revenge_result_strong_post_loss_deterioration"),
    REVENGE_CALCULATOR_CTA_CLICK("revenge_calculator_cta_click");

    private static final Set<String> VALUES = Arrays.stream(values()).map(SeoAcquisitionEventName::value).collect(Collectors.toUnmodifiableSet());
    private final String value;

    SeoAcquisitionEventName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isAllowed(String value) {
        return VALUES.contains(value);
    }
}
