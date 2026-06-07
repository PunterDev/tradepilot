package com.tradepilot.repository;

import com.tradepilot.entity.AdminAuditLog;
import com.tradepilot.enums.AdminAuditAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<AdminAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);

    List<AdminAuditLog> findTop6ByActionInOrderByCreatedAtDesc(List<AdminAuditAction> actions);

    List<AdminAuditLog> findTop10ByActionInOrderByCreatedAtDesc(List<AdminAuditAction> actions);

    long countByAction(AdminAuditAction action);
}
