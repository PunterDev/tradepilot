package com.tradepilot.survival.repository;

import com.tradepilot.survival.entity.NextSessionProtectionEvaluation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextSessionProtectionEvaluationRepository extends JpaRepository<NextSessionProtectionEvaluation, Long> {
    Optional<NextSessionProtectionEvaluation> findTopByCommitment_IdAndIsCurrentTrueOrderByEvaluatedSessionDateDescIdDesc(Long commitmentId);
    List<NextSessionProtectionEvaluation> findByCommitment_IdAndEvaluatedSessionDateAndIsCurrentTrue(
            Long commitmentId, LocalDate evaluatedSessionDate);
    List<NextSessionProtectionEvaluation> findByCommitment_IdOrderByIdDesc(
            Long commitmentId, Pageable pageable);
}
