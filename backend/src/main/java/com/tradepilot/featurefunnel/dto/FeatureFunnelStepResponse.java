package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;

public record FeatureFunnelStepResponse(
        String stepKey,
        String displayName,
        String description,
        FeatureKey feature,
        FeatureUsageAction action,
        FeatureUsageEventType eventType,
        int stepOrder,
        boolean required,
        boolean passive,
        long usersReached,
        double percentOfStarters,
        double percentFromPreviousStep,
        long dropoffCount,
        double dropoffRate,
        Double medianTimeFromPreviousHours,
        Double averageTimeFromPreviousHours,
        String planBreakdown,
        String newReturningBreakdown,
        String activePassiveClassification
) {}
