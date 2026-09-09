package com.tradepilot.preanalysis.repository;

import com.tradepilot.entity.User;
import com.tradepilot.preanalysis.entity.AiPreAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiPreAnalysisRepository extends JpaRepository<AiPreAnalysis, Long> {
    Optional<AiPreAnalysis> findByIdAndUser(Long id, User user);
    List<AiPreAnalysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<AiPreAnalysis> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);
}
