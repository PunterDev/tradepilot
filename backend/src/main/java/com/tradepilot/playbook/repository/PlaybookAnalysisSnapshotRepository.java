package com.tradepilot.playbook.repository;

import com.tradepilot.playbook.entity.PlaybookAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlaybookAnalysisSnapshotRepository extends JpaRepository<PlaybookAnalysisSnapshot, Long> {
    Optional<PlaybookAnalysisSnapshot> findBySetup_IdAndUser_Id(Long setupId, Long userId);
}
