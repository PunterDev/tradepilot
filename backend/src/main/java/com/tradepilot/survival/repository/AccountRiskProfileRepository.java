package com.tradepilot.survival.repository;

import com.tradepilot.entity.User;
import com.tradepilot.survival.entity.AccountRiskProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRiskProfileRepository extends JpaRepository<AccountRiskProfile, Long> {
    Optional<AccountRiskProfile> findByAccount_IdAndUser(Long accountId, User user);
}
