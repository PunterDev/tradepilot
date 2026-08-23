package com.tradepilot.featureusage;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FeatureUsageMetadataRegistry {
    private final Map<FeatureKey, FeatureUsageMetadata> metadata = new EnumMap<>(FeatureKey.class);

    public FeatureUsageMetadataRegistry() {
        register(new FeatureUsageMetadata(FeatureKey.TRADE_DETAIL, "Trade Detail", "Trade Detail", "Opening and reviewing individual trade records.", FeatureUsageCategory.JOURNALING, FeatureUsageEventType.PAGE_VIEW, true, false, false, true, 10, "file-text", "slate"));
        register(new FeatureUsageMetadata(FeatureKey.CALENDAR_VIEW, "PnL Calendar", "Calendar", "Calendar view of historical trading results.", FeatureUsageCategory.ANALYTICS, FeatureUsageEventType.ANALYTICS_VIEW, true, false, false, true, 20, "calendar", "cyan"));
        register(new FeatureUsageMetadata(FeatureKey.ANALYTICS_VIEW, "Analytics", "Analytics", "Core analytics dashboards and breakdowns.", FeatureUsageCategory.ANALYTICS, FeatureUsageEventType.ANALYTICS_VIEW, true, false, false, true, 30, "bar-chart", "blue"));
        register(new FeatureUsageMetadata(FeatureKey.AI_REVIEW, "AI Trade Review", "AI Review", "AI feedback for an individual trade.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 40, "sparkles", "emerald"));
        register(new FeatureUsageMetadata(FeatureKey.SESSION_REVIEW, "Session Review", "Session", "Review a full trading session and replay insights.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 50, "activity", "teal"));
        register(new FeatureUsageMetadata(FeatureKey.DAILY_COACH, "Daily Coach", "Daily Coach", "Date-specific AI coaching and next-session rule.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 60, "sun", "green"));
        register(new FeatureUsageMetadata(FeatureKey.WEEKLY_COACH, "Weekly Coach", "Weekly Coach", "Weekly AI performance review and focus areas.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 70, "calendar-days", "lime"));
        register(new FeatureUsageMetadata(FeatureKey.TRADING_DNA, "Trading DNA", "DNA", "Long-term trader profile and behavioral pattern analysis.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 80, "fingerprint", "violet"));
        register(new FeatureUsageMetadata(FeatureKey.AI_LEAK_DETECTOR, "AI Leak Detector", "Leak Detector", "Performance leak report with impact and recovery plan.", FeatureUsageCategory.PERFORMANCE, FeatureUsageEventType.AI_GENERATION, false, true, true, false, 90, "target", "rose"));
        register(new FeatureUsageMetadata(FeatureKey.PERFORMANCE_TIMELINE, "AI Performance Timeline", "Journey", "Timeline view of trading performance evolution.", FeatureUsageCategory.PERFORMANCE, FeatureUsageEventType.PAGE_VIEW, false, true, false, true, 100, "route", "amber"));
        register(new FeatureUsageMetadata(FeatureKey.PLAYBOOK, "AI Trading Playbook", "Playbook", "Setup documentation, checklist review, and behavior comparison.", FeatureUsageCategory.PLAYBOOK, FeatureUsageEventType.USER_ACTION, false, true, false, false, 110, "book-open", "teal"));
        register(new FeatureUsageMetadata(FeatureKey.TRADING_MEMORY, "AI Trading Memory", "Memory", "Searches similar reviewed trades and recurring patterns for historical trade context.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.USER_ACTION, false, true, false, false, 120, "brain", "cyan"));
        register(new FeatureUsageMetadata(FeatureKey.TRADE_REPLAY, "AI Trade Replay", "Replay", "One-minute futures replay with deterministic trade-management insights.", FeatureUsageCategory.AI_COACHING, FeatureUsageEventType.USER_ACTION, false, true, false, false, 130, "candlestick-chart", "sky"));
    }

    public FeatureUsageMetadata get(FeatureKey feature) {
        FeatureUsageMetadata item = metadata.get(feature);
        if (item != null) return item;
        String key = feature == null ? "UNKNOWN" : feature.name();
        return new FeatureUsageMetadata(feature, "Unknown feature: " + key, key, "Feature usage event has no metadata mapping.", FeatureUsageCategory.OTHER, FeatureUsageEventType.USER_ACTION, false, false, false, false, 999, "help-circle", "slate");
    }

    public List<FeatureUsageMetadata> all() {
        return Arrays.stream(FeatureKey.values())
                .map(this::get)
                .sorted(Comparator.comparingInt(FeatureUsageMetadata::sortOrder))
                .toList();
    }

    public boolean isKnown(FeatureKey feature) {
        return metadata.containsKey(feature);
    }

    private void register(FeatureUsageMetadata item) {
        metadata.put(item.key(), item);
    }
}
