package com.tradepilot.playbook.repository;

import com.tradepilot.playbook.entity.PlaybookMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaybookMilestoneRepository extends JpaRepository<PlaybookMilestone, Long> {
    boolean existsByUser_IdAndSetup_IdAndMilestoneType(Long userId, Long setupId, String milestoneType);
    List<PlaybookMilestone> findTop10ByUser_IdOrderByEarnedDateDescCreatedAtDesc(Long userId);
    List<PlaybookMilestone> findBySetup_IdOrderByEarnedDateDescCreatedAtDesc(Long setupId);
}
