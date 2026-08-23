package com.tradepilot.playbook.repository;

import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.enums.PlaybookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PlaybookSetupRepository extends JpaRepository<PlaybookSetup, Long> {
    @EntityGraph(attributePaths = {"account"})
    Optional<PlaybookSetup> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"account"})
    @Query("""
            select s from PlaybookSetup s
            where s.user.id = :userId
              and (:accountId is null or s.account.id = :accountId)
              and (:status is null or s.status = :status)
            order by s.updatedAt desc, s.id desc
            """)
    Page<PlaybookSetup> search(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("status") PlaybookStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"account"})
    @Query("""
            select s from PlaybookSetup s
            where s.user.id = :userId
              and (:accountId is null or s.account.id = :accountId)
              and (:status is null or s.status = :status)
              and lower(s.name) like :searchPattern
            order by s.updatedAt desc, s.id desc
            """)
    Page<PlaybookSetup> searchByName(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("status") PlaybookStatus status,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );

    long countByUser_IdAndStatusNot(Long userId, PlaybookStatus status);
    long countByUser_IdAndStatus(Long userId, PlaybookStatus status);
}
