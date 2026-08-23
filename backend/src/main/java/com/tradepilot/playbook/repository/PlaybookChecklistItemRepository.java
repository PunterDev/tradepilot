package com.tradepilot.playbook.repository;

import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaybookChecklistItemRepository extends JpaRepository<PlaybookChecklistItem, Long> {
    List<PlaybookChecklistItem> findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(Long setupId);
    long countBySetup_IdAndActiveTrue(Long setupId);
    void deleteBySetup_Id(Long setupId);
}
