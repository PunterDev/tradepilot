package com.tradepilot.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface StripeCheckoutAttemptRepository extends JpaRepository<StripeCheckoutAttempt, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StripeCheckoutAttempt> findByUserId(Long userId);
}
