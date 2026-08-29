package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiCreditPurchaseRepository extends JpaRepository<AiCreditPurchase, Long> {
    boolean existsByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<AiCreditPurchase> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<AiCreditPurchase> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<AiCreditPurchase> findByStripeChargeId(String stripeChargeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AiCreditPurchase p where p.stripePaymentIntentId = :stripePaymentIntentId")
    Optional<AiCreditPurchase> findByStripePaymentIntentIdForUpdate(@Param("stripePaymentIntentId") String stripePaymentIntentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AiCreditPurchase p where p.stripeChargeId = :stripeChargeId")
    Optional<AiCreditPurchase> findByStripeChargeIdForUpdate(@Param("stripeChargeId") String stripeChargeId);

    @Query("select count(p) from AiCreditPurchase p where p.status = com.tradepilot.billing.AiCreditPurchaseStatus.COMPLETED and p.completedAt >= :start and p.completedAt < :end")
    long countCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
