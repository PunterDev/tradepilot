package com.tradepilot.playbook.service;

import com.tradepilot.playbook.dto.PlaybookBehaviorComparisonResponse;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.ChecklistResult;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class PlaybookBehaviorComparisonService {
    private final PlaybookJsonService jsonService;

    public PlaybookBehaviorComparisonService(PlaybookJsonService jsonService) {
        this.jsonService = jsonService;
    }

    public List<PlaybookBehaviorComparisonResponse> compare(List<PlaybookChecklistItem> items, List<TradePlaybookAssignment> assignments) {
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()) && Boolean.TRUE.equals(item.getRequired()))
                .map(item -> compareRule(item, assignments))
                .toList();
    }

    private PlaybookBehaviorComparisonResponse compareRule(PlaybookChecklistItem item, List<TradePlaybookAssignment> assignments) {
        List<TradePlaybookAssignment> met = assignments.stream().filter(a -> result(a, item.getId()) == ChecklistResult.MET).toList();
        List<TradePlaybookAssignment> notMet = assignments.stream().filter(a -> result(a, item.getId()) == ChecklistResult.NOT_MET).toList();
        BigDecimal metAvg = averageR(met);
        BigDecimal notMetAvg = averageR(notMet);
        BigDecimal metWin = winRate(met);
        BigDecimal notMetWin = winRate(notMet);
        boolean meaningful = met.size() >= 5 && notMet.size() >= 5 && metAvg != null && notMetAvg != null;
        String confidence = meaningful ? "MEDIUM" : "INSUFFICIENT";
        String interpretation = meaningful
                ? String.format("Historically associated with %sR when met versus %sR when missed. This is association, not causation.", metAvg, notMetAvg)
                : "Insufficient reviewed trades to compare this rule safely.";
        return new PlaybookBehaviorComparisonResponse(item.getId(), item.getLabel(), met.size(), notMet.size(), metAvg, notMetAvg, metWin, notMetWin, confidence, meaningful, interpretation);
    }

    private ChecklistResult result(TradePlaybookAssignment assignment, Long itemId) {
        Map<Long, String> map = jsonService.readChecklistResults(assignment.getChecklistResultsJson());
        try {
            return ChecklistResult.valueOf(map.getOrDefault(itemId, "UNKNOWN"));
        } catch (Exception ex) {
            return ChecklistResult.UNKNOWN;
        }
    }

    private BigDecimal averageR(List<TradePlaybookAssignment> assignments) {
        if (assignments.isEmpty()) return null;
        BigDecimal total = assignments.stream().map(this::r).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(assignments.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal winRate(List<TradePlaybookAssignment> assignments) {
        if (assignments.isEmpty()) return null;
        long wins = assignments.stream().filter(a -> a.getTrade().getPnlAmount() != null && a.getTrade().getPnlAmount().compareTo(BigDecimal.ZERO) > 0).count();
        return BigDecimal.valueOf(wins).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(assignments.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal r(TradePlaybookAssignment assignment) {
        if (assignment.getTrade().getPnlR() != null) return assignment.getTrade().getPnlR();
        if (assignment.getTrade().getPnlAmount() == null) return BigDecimal.ZERO;
        BigDecimal risk = assignment.getTrade().getRiskAmount();
        if (risk == null && assignment.getTrade().getAccount() != null) risk = assignment.getTrade().getAccount().getDefaultRiskAmount();
        if (risk == null || risk.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return assignment.getTrade().getPnlAmount().divide(risk, 2, RoundingMode.HALF_UP);
    }
}
