package com.tradepilot.playbook.service;

import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

@Service
public class PlaybookHashService {
    public static final String CALCULATION_VERSION = "playbook-calc-v1";
    public static final String PROMPT_VERSION = "playbook-prompt-v1";

    public String hash(PlaybookSetup setup, List<PlaybookChecklistItem> items, List<TradePlaybookAssignment> assignments) {
        StringBuilder builder = new StringBuilder();
        builder.append(setup.getId()).append('|')
                .append(setup.getUpdatedAt()).append('|')
                .append(setup.getVersionNumber()).append('|')
                .append(setup.getStatus()).append('|')
                .append(setup.getDescription()).append('|')
                .append(setup.getMarketContextJson()).append('|')
                .append(setup.getRiskRulesJson()).append('|')
                .append(setup.getManagementRulesJson()).append('|')
                .append(CALCULATION_VERSION).append('|')
                .append(PROMPT_VERSION);
        items.stream().sorted(Comparator.comparing(PlaybookChecklistItem::getId, Comparator.nullsLast(Long::compareTo))).forEach(item ->
                builder.append("|item:").append(item.getId()).append(':').append(item.getChecklistType()).append(':').append(item.getLabel()).append(':').append(item.getRequired()).append(':').append(item.getWeight()).append(':').append(item.getActive()));
        assignments.stream().sorted(Comparator.comparing(a -> a.getTrade().getId())).forEach(assignment ->
                builder.append("|assignment:").append(assignment.getId())
                        .append(':').append(assignment.getTrade().getId())
                        .append(':').append(assignment.getTrade().getUpdatedAt())
                        .append(':').append(assignment.getTrade().getPnlAmount())
                        .append(':').append(assignment.getTrade().getPnlR())
                        .append(':').append(assignment.getTrade().getSide())
                        .append(':').append(assignment.getTrade().getSetup())
                        .append(':').append(assignment.getTrade().getConfidence())
                        .append(':').append(assignment.getTrade().getEmotion())
                        .append(':').append(assignment.getSetupQualityGrade())
                        .append(':').append(assignment.getExecutionQualityGrade())
                        .append(':').append(assignment.getChecklistResultsJson())
                        .append(':').append(assignment.getRuleViolationsJson())
                        .append(':').append(assignment.getReviewedByUser()));
        return sha256(builder.toString());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash playbook data.", ex);
        }
    }
}
