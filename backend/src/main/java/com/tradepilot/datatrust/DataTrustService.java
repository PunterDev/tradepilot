package com.tradepilot.datatrust;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.datatrust.dto.*;
import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.leakdetector.dto.BehavioralFindingDto;
import com.tradepilot.leakdetector.dto.LeakDetectorResponse;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataTrustService {
    private final ImportBatchRepository batches; private final TradeRepository trades; private final ImportReconciliationRepository reconciliations; private final AiLeakDetectorSnapshotRepository snapshots; private final InsightFeedbackRepository feedback; private final TradingAccountRepository accounts; private final ObjectMapper objectMapper;
    public DataTrustService(ImportBatchRepository batches, TradeRepository trades, ImportReconciliationRepository reconciliations, AiLeakDetectorSnapshotRepository snapshots, InsightFeedbackRepository feedback, TradingAccountRepository accounts, ObjectMapper objectMapper) { this.batches=batches; this.trades=trades; this.reconciliations=reconciliations; this.snapshots=snapshots; this.feedback=feedback; this.accounts=accounts; this.objectMapper=objectMapper; }
    @Transactional public ImportReconciliationResponse createForImport(User user, ImportBatch batch, String source) {
        ImportReconciliation value = new ImportReconciliation(); value.setUser(user); value.setAccount(batch.getAccount()); value.setImportBatch(batch); value.setSource(source); value.setTradeCount(batch.getImportedRows()); value.setNetPnl(trades.findByImportBatch_Id(batch.getId()).stream().map(t -> t.getPnlAmount()==null?BigDecimal.ZERO:t.getPnlAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)); return map(reconciliations.save(value));
    }
    @Transactional(readOnly=true) public ImportReconciliationResponse reconciliation(User user, Long batchId) { return map(requireReconciliation(user,batchId)); }
    @Transactional public ImportReconciliationResponse updateReconciliation(User user, Long batchId, ReconciliationUpdateRequest request) {
        if (request.status()==ImportReconciliationStatus.MISMATCH_REPORTED && (request.mismatchReasons()==null || request.mismatchReasons().isEmpty())) throw new IllegalArgumentException("Select at least one mismatch reason.");
        ImportReconciliation value=requireReconciliation(user,batchId); value.setStatus(request.status());
        if (request.status() == ImportReconciliationStatus.MATCHED) { value.setMismatchReasons(null); value.setComment(null); }
        else { value.setMismatchReasons(request.mismatchReasons().stream().map(Enum::name).sorted().reduce((a,b)->a+","+b).orElse(null)); value.setComment(trim(request.comment())); }
        value.setReviewedAt(LocalDateTime.now()); return map(value);
    }
    @Transactional(readOnly=true) public InsightFeedbackResponse feedback(User user, Long snapshotId, String findingType) { return feedback.findByUser_IdAndSnapshot_IdAndFindingType(user.getId(),snapshotId,findingType).map(this::map).orElse(null); }
    @Transactional public InsightFeedbackResponse updateFeedback(User user, Long snapshotId, String findingType, InsightFeedbackRequest request) {
        Finding finding = requireFinding(user,snapshotId,findingType); if (!request.helpful() && request.reason()==null) throw new IllegalArgumentException("Select a feedback reason.");
        InsightFeedback value=feedback.findByUser_IdAndSnapshot_IdAndFindingType(user.getId(),snapshotId,findingType).orElseGet(InsightFeedback::new);
        value.setUser(user); value.setAccount(accounts.findById(finding.snapshot().getAccountId()).orElseThrow(()->new ResourceNotFoundException("Account not found"))); value.setSnapshot(finding.snapshot()); value.setFindingType(findingType); value.setEngineVersion(finding.value().engineVersion()); value.setRuleVersion(finding.value().ruleVersion()); value.setEvidenceStrength(finding.value().evidenceStrength().name()); value.setHelpful(request.helpful()); value.setReason(request.helpful()?null:request.reason()); value.setComment(trim(request.comment()));
        return map(feedback.save(value));
    }
    private ImportReconciliation requireReconciliation(User user,Long batchId){ return reconciliations.findByImportBatch_IdAndUser_Id(batchId,user.getId()).orElseThrow(()->new ResourceNotFoundException("Import reconciliation not found")); }
    private Finding requireFinding(User user, Long snapshotId,String findingType){ try { AiLeakDetectorSnapshot snapshot=snapshots.findById(snapshotId).filter(s->Objects.equals(s.getUserId(),user.getId())).orElseThrow(()->new ResourceNotFoundException("Insight not found")); LeakDetectorResponse response=objectMapper.readValue(snapshot.getResultJson(),LeakDetectorResponse.class); LeakResultDto leak=response.getLeaks().stream().filter(l->l.getLeakType()!=null && l.getLeakType().name().equals(findingType) && l.getBehavioralFinding()!=null).findFirst().orElseThrow(()->new ResourceNotFoundException("Insight not found")); BehavioralFindingDto finding=leak.getBehavioralFinding(); return new Finding(snapshot,finding); } catch(ResourceNotFoundException ex){throw ex;} catch(Exception ex){throw new ResourceNotFoundException("Insight not found");} }
    private ImportReconciliationResponse map(ImportReconciliation value){ Set<ImportMismatchReason> reasons=value.getMismatchReasons()==null||value.getMismatchReasons().isBlank()?Set.of():Arrays.stream(value.getMismatchReasons().split(",")).map(ImportMismatchReason::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet()); return new ImportReconciliationResponse(value.getImportBatch().getId(),value.getSource(),value.getTradeCount(),value.getNetPnl(),value.getStatus(),value.getReviewedAt(),reasons,value.getComment()); }
    private InsightFeedbackResponse map(InsightFeedback value){ return new InsightFeedbackResponse(value.getSnapshot().getId(),value.getFindingType(),value.isHelpful(),value.getReason(),value.getUpdatedAt()); }
    private String trim(String input){return input==null?null:input.trim().isEmpty()?null:input.trim();} private record Finding(AiLeakDetectorSnapshot snapshot, BehavioralFindingDto value){}
}
