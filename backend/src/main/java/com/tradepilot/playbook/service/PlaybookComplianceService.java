package com.tradepilot.playbook.service;

import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.enums.ChecklistResult;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class PlaybookComplianceService {
    private static final BigDecimal REQUIRED_WEIGHT = BigDecimal.valueOf(70);
    private static final BigDecimal OPTIONAL_WEIGHT = BigDecimal.valueOf(30);

    public PlaybookComplianceResult score(List<PlaybookChecklistItem> items, Map<Long, String> results) {
        List<PlaybookChecklistItem> active = items.stream().filter(item -> Boolean.TRUE.equals(item.getActive())).toList();
        if (active.isEmpty()) {
            return new PlaybookComplianceResult(0, 0, 0, 0, 0, PlaybookConfidence.INSUFFICIENT);
        }
        PlaybookComplianceGroupScore required = scoreGroup(active.stream().filter(item -> Boolean.TRUE.equals(item.getRequired())).toList(), results);
        PlaybookComplianceGroupScore optional = scoreGroup(active.stream().filter(item -> !Boolean.TRUE.equals(item.getRequired())).toList(), results);
        int requiredPart = required.availableWeight() == 0 ? 0 : weighted(required.metWeight(), required.availableWeight(), REQUIRED_WEIGHT);
        int optionalPart = optional.availableWeight() == 0 ? 0 : weighted(optional.metWeight(), optional.availableWeight(), OPTIONAL_WEIGHT);
        int penalty = required.notMetCount() * 5;
        int score = clamp(requiredPart + optionalPart - penalty);
        int reviewed = active.size() - required.unknownCount() - optional.unknownCount();
        BigDecimal reviewedRate = BigDecimal.valueOf(reviewed).divide(BigDecimal.valueOf(active.size()), 4, RoundingMode.HALF_UP);
        PlaybookConfidence confidence = confidence(reviewedRate, required.unknownCount() + optional.unknownCount(), active.size());
        return new PlaybookComplianceResult(score, clamp(requiredPart), clamp(optionalPart), required.notMetCount(), required.unknownCount() + optional.unknownCount(), confidence);
    }

    private PlaybookComplianceGroupScore scoreGroup(List<PlaybookChecklistItem> items, Map<Long, String> results) {
        int available = 0;
        int met = 0;
        int notMet = 0;
        int unknown = 0;
        for (PlaybookChecklistItem item : items) {
            ChecklistResult result = parse(results.get(item.getId()));
            int weight = Math.max(1, item.getWeight() == null ? 1 : item.getWeight());
            if (result == ChecklistResult.NOT_APPLICABLE) continue;
            available += weight;
            if (result == ChecklistResult.MET) met += weight;
            if (result == ChecklistResult.NOT_MET) notMet++;
            if (result == ChecklistResult.UNKNOWN) unknown++;
        }
        return new PlaybookComplianceGroupScore(available, met, notMet, unknown);
    }

    private int weighted(int met, int available, BigDecimal sectionWeight) {
        return BigDecimal.valueOf(met).multiply(sectionWeight).divide(BigDecimal.valueOf(available), 0, RoundingMode.HALF_UP).intValue();
    }

    private ChecklistResult parse(String value) {
        if (value == null || value.isBlank()) return ChecklistResult.UNKNOWN;
        try {
            return ChecklistResult.valueOf(value);
        } catch (Exception ignored) {
            return ChecklistResult.UNKNOWN;
        }
    }

    private PlaybookConfidence confidence(BigDecimal reviewedRate, int unknown, int total) {
        if (total == 0) return PlaybookConfidence.INSUFFICIENT;
        if (reviewedRate.compareTo(BigDecimal.valueOf(0.95)) >= 0) return PlaybookConfidence.HIGH;
        if (reviewedRate.compareTo(BigDecimal.valueOf(0.70)) >= 0) return PlaybookConfidence.MEDIUM;
        if (unknown < total) return PlaybookConfidence.LOW;
        return PlaybookConfidence.INSUFFICIENT;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

}
