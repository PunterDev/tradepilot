package com.tradepilot.survival.repository;

import com.tradepilot.entity.User;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import com.tradepilot.survival.ProtectionCommitmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NextSessionProtectionCommitmentRepository extends JpaRepository<NextSessionProtectionCommitment, Long> {
    Optional<NextSessionProtectionCommitment> findByAccount_IdAndUserAndEffectiveSessionDate(Long accountId, User user, LocalDate date);
    List<NextSessionProtectionCommitment> findByAccount_IdAndUserAndStatusNot(Long accountId, User user, ProtectionCommitmentStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select commitment from NextSessionProtectionCommitment commitment where commitment.account.id = :accountId and commitment.user = :user and commitment.effectiveSessionDate = :date")
    Optional<NextSessionProtectionCommitment> findForUpdate(@Param("accountId") Long accountId, @Param("user") User user, @Param("date") LocalDate date);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select commitment from NextSessionProtectionCommitment commitment where commitment.account.id = :accountId and commitment.user = :user and commitment.effectiveSessionDate = :date")
    Optional<NextSessionProtectionCommitment> findForEvaluation(@Param("accountId") Long accountId, @Param("user") User user, @Param("date") LocalDate date);
}
