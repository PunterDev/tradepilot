package com.tradepilot.repository;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    void deleteByAccountIdAndUser(Long accountId, User user);
}
