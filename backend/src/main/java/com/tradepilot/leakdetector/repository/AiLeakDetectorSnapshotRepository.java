package com.tradepilot.leakdetector.repository;

import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiLeakDetectorSnapshotRepository extends JpaRepository<AiLeakDetectorSnapshot, Long> {
    @Query("""
            select s
            from AiLeakDetectorSnapshot s
            where s.userId = :userId
              and s.accountId = :accountId
              and s.dataHash = :dataHash
            order by s.updatedAt desc
            """)
    List<AiLeakDetectorSnapshot> findMatchingSnapshot(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("dataHash") String dataHash,
            Pageable pageable
    );

    Optional<AiLeakDetectorSnapshot> findTopByUserIdAndAccountIdOrderByUpdatedAtDesc(Long userId, Long accountId);

    List<AiLeakDetectorSnapshot> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
