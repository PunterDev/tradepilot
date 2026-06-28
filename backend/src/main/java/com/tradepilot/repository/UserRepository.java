package com.tradepilot.repository;

import com.tradepilot.entity.User;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.enums.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    Optional<User> findByStripeSubscriptionId(String stripeSubscriptionId);

    long countByStripeCustomerIdIsNotNull();

    long countByStripeSubscriptionIdIsNotNull();

    long countByPlanAndSubscriptionStatusInAndStripeSubscriptionIdIsNotNull(
            PlanType plan,
            List<SubscriptionStatus> subscriptionStatuses
    );

    List<User> findByStripeCustomerIdIsNotNull();

    boolean existsByEmail(String email);

    List<User> findTop100ByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCreatedAtDesc(String email, String name);

    List<User> findTop100ByOrderByCreatedAtDesc();

    Page<User> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name, Pageable pageable);

    long countByPlan(PlanType plan);

    long countByRole(UserRole role);

    long countByEmailVerified(Boolean emailVerified);

    List<User> findTop8ByOrderByCreatedAtDesc();

    List<User> findTop10ByOrderByCreatedAtDesc();

    @Query("""
            select count(u),
                   sum(case when u.plan = com.tradepilot.billing.PlanType.FREE then 1 else 0 end),
                   sum(case when u.plan = com.tradepilot.billing.PlanType.PRO then 1 else 0 end),
                   sum(case when u.role = com.tradepilot.enums.UserRole.ADMIN then 1 else 0 end),
                   sum(case when u.emailVerified = true then 1 else 0 end),
                   sum(case when u.emailVerified = false then 1 else 0 end)
            from User u
            """)
    Object adminUserCounts();
}
