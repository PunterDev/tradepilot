package com.tradepilot.manualbacktest.repository;

import com.tradepilot.manualbacktest.entity.ManualBacktestSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ManualBacktestSessionRepository extends JpaRepository<ManualBacktestSession, Long> {
    Optional<ManualBacktestSession> findByIdAndUserId(Long id, Long userId);
    List<ManualBacktestSession> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ManualBacktestSession session where session.id = :id and session.userId = :userId")
    Optional<ManualBacktestSession> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
