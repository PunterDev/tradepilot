package com.tradepilot.billing;

import com.tradepilot.entity.User;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class FeatureAccessService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureAccessService.class);
    private static final Set<Feature> FREE_FEATURES = Set.of(Feature.CSV_IMPORT, Feature.PNL_CALENDAR, Feature.BASIC_ANALYTICS, Feature.AI_TRADE_REVIEW);

    public boolean hasFeature(User user, Feature feature) {
        if (isPro(user)) {
            return true;
        }
        return FREE_FEATURES.contains(feature);
    }

    public void requireFeature(User user, Feature feature) {
        if (hasFeature(user, feature)) {
            return;
        }
        log.warn("Feature access blocked. userId={}, plan={}, feature={}", user == null ? null : user.getId(), plan(user), feature);
        throw new FeatureAccessException(feature, "Upgrade to Pro to access " + label(feature) + ".");
    }

    public boolean isPro(User user) {
        return plan(user) == PlanType.PRO;
    }

    private PlanType plan(User user) {
        return user == null || user.getPlan() == null ? PlanType.FREE : user.getPlan();
    }

    private String label(Feature feature) {
        return switch (feature) {
            case CSV_IMPORT -> "CSV Import";
            case PNL_CALENDAR -> "PnL Calendar";
            case BASIC_ANALYTICS -> "Basic Analytics";
            case AI_TRADE_REVIEW -> "AI Trade Review";
            case SESSION_REVIEW -> "Session Review";
            case DAILY_COACH -> "Daily Coach";
            case WEEKLY_COACH -> "Weekly Coach";
            case TRADING_DNA -> "Trading DNA";
            case AI_LEAK_DETECTOR -> "AI Leak Detector";
            case PERFORMANCE_TIMELINE -> "AI Performance Timeline";
            case PLAYBOOK -> "AI Trading Playbook";
            case TRADING_MEMORY -> "AI Trading Memory";
            case TRADE_REPLAY -> "AI Trade Replay";
            case ADVANCED_ANALYTICS -> "Advanced Analytics";
        };
    }
}
