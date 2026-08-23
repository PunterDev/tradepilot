package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiCreditPurchaseRepository extends JpaRepository<AiCreditPurchase, Long> {
    boolean existsByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<AiCreditPurchase> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Query("select count(p) from AiCreditPurchase p where p.status = com.tradepilot.billing.AiCreditPurchaseStatus.COMPLETED and p.completedAt >= :start and p.completedAt < :end")
    long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
