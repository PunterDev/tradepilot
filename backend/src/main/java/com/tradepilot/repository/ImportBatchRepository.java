package com.tradepilot.repository;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    void deleteByAccountIdAndUser(Long accountId, User user);

    Optional<ImportBatch> findTopByUserAndImportedRowsGreaterThanOrderByImportedAtAsc(User user, Integer importedRows);
}
