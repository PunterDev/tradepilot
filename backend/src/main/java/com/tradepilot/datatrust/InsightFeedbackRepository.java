package com.tradepilot.datatrust;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface InsightFeedbackRepository extends JpaRepository<InsightFeedback, Long> { Optional<InsightFeedback> findByUser_IdAndSnapshot_IdAndFindingType(Long userId, Long snapshotId, String findingType); }
