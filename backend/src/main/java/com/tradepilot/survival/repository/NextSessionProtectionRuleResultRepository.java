package com.tradepilot.survival.repository;

import com.tradepilot.survival.entity.NextSessionProtectionRuleResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextSessionProtectionRuleResultRepository extends JpaRepository<NextSessionProtectionRuleResult, Long> {
    List<NextSessionProtectionRuleResult> findByEvaluation_IdOrderById(Long evaluationId);
    List<NextSessionProtectionRuleResult> findByEvaluation_IdInOrderByEvaluation_IdAscIdAsc(List<Long> evaluationIds);
}
