package com.tradepilot.datatrust;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.datatrust.dto.ReconciliationUpdateRequest;
import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.User;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.leakdetector.dto.*;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DataTrustServiceTests {
  @Test void reconciliationsReviseInPlaceAndMatchedClearsStaleMismatchData() {
    ImportReconciliationRepository reconciliations=mock(ImportReconciliationRepository.class); User user=new User(); user.setId(7L); ImportReconciliation row=row(user,44L);
    when(reconciliations.findByImportBatch_IdAndUser_Id(44L,7L)).thenReturn(Optional.of(row));
    DataTrustService service=service(reconciliations);
    var mismatch=service.updateReconciliation(user,44L,new ReconciliationUpdateRequest(ImportReconciliationStatus.MISMATCH_REPORTED,Set.of(ImportMismatchReason.PNL_MISMATCH,ImportMismatchReason.TRADE_COUNT_MISMATCH),"plain text"));
    assertThat(mismatch.status()).isEqualTo(ImportReconciliationStatus.MISMATCH_REPORTED); assertThat(mismatch.mismatchReasons()).containsExactlyInAnyOrder(ImportMismatchReason.PNL_MISMATCH,ImportMismatchReason.TRADE_COUNT_MISMATCH); assertThat(mismatch.comment()).isEqualTo("plain text"); assertThat(mismatch.tradeCount()).isEqualTo(32); assertThat(mismatch.netPnl()).isEqualByComparingTo("664.30");
    var matched=service.updateReconciliation(user,44L,new ReconciliationUpdateRequest(ImportReconciliationStatus.MATCHED,Set.of(ImportMismatchReason.PNL_MISMATCH),"ignored"));
    assertThat(matched.status()).isEqualTo(ImportReconciliationStatus.MATCHED); assertThat(matched.mismatchReasons()).isEmpty(); assertThat(matched.comment()).isNull(); assertThat(row.getReviewedAt()).isNotNull(); verify(reconciliations,times(2)).findByImportBatch_IdAndUser_Id(44L,7L);
  }
  @Test void rejectsMissingReasonsAndCrossUserAccess() {
    ImportReconciliationRepository reconciliations=mock(ImportReconciliationRepository.class); User owner=new User();owner.setId(1L); User other=new User();other.setId(2L); when(reconciliations.findByImportBatch_IdAndUser_Id(44L,2L)).thenReturn(Optional.empty()); DataTrustService service=service(reconciliations);
    assertThatThrownBy(()->service.updateReconciliation(other,44L,new ReconciliationUpdateRequest(ImportReconciliationStatus.MISMATCH_REPORTED,Set.of(),null))).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->service.reconciliation(other,44L)).isInstanceOf(RuntimeException.class);
  }
  @Test void feedbackUsesSnapshotMetadataAndRevisesOneCurrentRecord() throws Exception {
    InsightFeedbackRepository feedback=mock(InsightFeedbackRepository.class); AiLeakDetectorSnapshotRepository snapshots=mock(AiLeakDetectorSnapshotRepository.class); TradingAccountRepository accounts=mock(TradingAccountRepository.class); User user=new User();user.setId(7L); TradingAccount account=new TradingAccount();account.setId(3L); AiLeakDetectorSnapshot snapshot=snapshot(user,3L); when(snapshots.findById(9L)).thenReturn(Optional.of(snapshot)); when(accounts.findById(3L)).thenReturn(Optional.of(account)); when(feedback.findByUser_IdAndSnapshot_IdAndFindingType(7L,9L,"POST_LOSS_OVERTRADING")).thenReturn(Optional.empty()); when(feedback.save(any())).thenAnswer(i->i.getArgument(0));
    DataTrustService service=new DataTrustService(mock(ImportBatchRepository.class),mock(TradeRepository.class),mock(ImportReconciliationRepository.class),snapshots,feedback,accounts,new ObjectMapper());
    var positive=service.updateFeedback(user,9L,"POST_LOSS_OVERTRADING",new com.tradepilot.datatrust.dto.InsightFeedbackRequest(true,null,null)); assertThat(positive.helpful()).isTrue();
    InsightFeedback existing=new InsightFeedback();existing.setUser(user);existing.setSnapshot(snapshot);existing.setFindingType("POST_LOSS_OVERTRADING");when(feedback.findByUser_IdAndSnapshot_IdAndFindingType(7L,9L,"POST_LOSS_OVERTRADING")).thenReturn(Optional.of(existing));
    service.updateFeedback(user,9L,"POST_LOSS_OVERTRADING",new com.tradepilot.datatrust.dto.InsightFeedbackRequest(false,InsightFeedbackReason.WRONG_CONCLUSION,"optional")); assertThat(existing.getEngineVersion()).isEqualTo("insight-trust-v1"); assertThat(existing.getRuleVersion()).isEqualTo("post-loss-overtrading-v1"); assertThat(existing.getEvidenceStrength()).isEqualTo("STRONG"); assertThat(existing.getReason()).isEqualTo(InsightFeedbackReason.WRONG_CONCLUSION); verify(feedback,times(2)).save(any());
  }
  @Test void feedbackReadAndWriteAreScopedToTheAuthenticatedOwner() throws Exception {
    InsightFeedbackRepository feedback=mock(InsightFeedbackRepository.class); AiLeakDetectorSnapshotRepository snapshots=mock(AiLeakDetectorSnapshotRepository.class); TradingAccountRepository accounts=mock(TradingAccountRepository.class); User owner=new User();owner.setId(7L); User other=new User();other.setId(8L); AiLeakDetectorSnapshot owned=snapshot(owner,3L);
    when(feedback.findByUser_IdAndSnapshot_IdAndFindingType(8L,9L,"POST_LOSS_OVERTRADING")).thenReturn(Optional.empty()); when(snapshots.findById(9L)).thenReturn(Optional.of(owned));
    DataTrustService service=new DataTrustService(mock(ImportBatchRepository.class),mock(TradeRepository.class),mock(ImportReconciliationRepository.class),snapshots,feedback,accounts,new ObjectMapper());
    assertThat(service.feedback(other,9L,"POST_LOSS_OVERTRADING")).isNull();
    assertThatThrownBy(()->service.updateFeedback(other,9L,"POST_LOSS_OVERTRADING",new com.tradepilot.datatrust.dto.InsightFeedbackRequest(true,null,null))).isInstanceOf(com.tradepilot.exception.ResourceNotFoundException.class);
    verify(feedback).findByUser_IdAndSnapshot_IdAndFindingType(8L,9L,"POST_LOSS_OVERTRADING"); verifyNoInteractions(accounts);
  }
  private DataTrustService service(ImportReconciliationRepository r){return new DataTrustService(mock(ImportBatchRepository.class),mock(TradeRepository.class),r,mock(AiLeakDetectorSnapshotRepository.class),mock(InsightFeedbackRepository.class),mock(TradingAccountRepository.class),new ObjectMapper());}
  private ImportReconciliation row(User user,long id){ImportBatch batch=new ImportBatch();batch.setId(id);ImportReconciliation value=new ImportReconciliation();value.setUser(user);value.setImportBatch(batch);value.setSource("TRADOVATE");value.setTradeCount(32);value.setNetPnl(new BigDecimal("664.30"));return value;}
  private AiLeakDetectorSnapshot snapshot(User user,long accountId) throws Exception { LeakResultDto leak=LeakResultDto.of(LeakType.POST_LOSS_OVERTRADING,"","", "",0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,true,0,LeakSeverity.LOW,"",0,List.of(),null); leak.setBehavioralFinding(new BehavioralFindingDto(true,null,2,10,5,EvidenceStrength.STRONG,new BehavioralBaselineDto(8,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),new BehavioralBaselineDto(2,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),new BehavioralCounterfactualDto(false,false,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),"insight-trust-v1","post-loss-overtrading-v1",null)); LeakDetectorResponse response=new LeakDetectorResponse();response.setLeaks(List.of(leak)); AiLeakDetectorSnapshot result=new AiLeakDetectorSnapshot();result.setId(9L);result.setUserId(user.getId());result.setAccountId(accountId);result.setResultJson(new ObjectMapper().writeValueAsString(response));return result;}
}
