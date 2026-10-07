package com.tradepilot.datatrust;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ImportReconciliationRepository extends JpaRepository<ImportReconciliation, Long> { Optional<ImportReconciliation> findByImportBatch_IdAndUser_Id(Long batchId, Long userId); }
