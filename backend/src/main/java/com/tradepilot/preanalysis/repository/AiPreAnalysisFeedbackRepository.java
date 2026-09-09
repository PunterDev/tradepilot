package com.tradepilot.preanalysis.repository;

import com.tradepilot.entity.User;
import com.tradepilot.preanalysis.entity.AiPreAnalysis;
import com.tradepilot.preanalysis.entity.AiPreAnalysisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiPreAnalysisFeedbackRepository extends JpaRepository<AiPreAnalysisFeedback, Long> {
    Optional<AiPreAnalysisFeedback> findByAnalysisAndUser(AiPreAnalysis analysis, User user);
}
