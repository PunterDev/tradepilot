package com.tradepilot.repository;

import com.tradepilot.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    void deleteByAccountId(Long accountId);
}