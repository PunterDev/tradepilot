package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.DataTrustQualityResponse;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminDataTrustServiceTests {
  @Test void aggregatesExactEligibleFixtureAndExcludesAdminAndOptedOutUsersEverywhere() {
    ImportReconciliationRepository reconciliations = mock(ImportReconciliationRepository.class);
    InsightFeedbackRepository feedback = mock(InsightFeedbackRepository.class);
    User user = user(UserRole.USER, false), admin = user(UserRole.ADMIN, false), excluded = user(UserRole.USER, true);

    List<ImportReconciliation> imports = new ArrayList<>();
    for (int index = 0; index < 8; index++) imports.add(reconciliation(user, ImportReconciliationStatus.MATCHED, "TRADOVATE", index == 0 ? "PNL_MISMATCH" : null));
    imports.add(reconciliation(user, ImportReconciliationStatus.MISMATCH_REPORTED, "TRADOVATE", "PNL_MISMATCH"));
    imports.add(reconciliation(user, ImportReconciliationStatus.MISMATCH_REPORTED, "TOPSTEP", "TRADE_COUNT_MISMATCH"));
    imports.add(reconciliation(user, ImportReconciliationStatus.NOT_REVIEWED, "TOPSTEP", null));
    imports.add(reconciliation(user, ImportReconciliationStatus.NOT_REVIEWED, "TOPSTEP", null));
    imports.add(reconciliation(admin, ImportReconciliationStatus.MISMATCH_REPORTED, "ADMIN_ONLY", "OTHER"));
    imports.add(reconciliation(excluded, ImportReconciliationStatus.MATCHED, "EXCLUDED_ONLY", "OTHER"));
    when(reconciliations.findAll()).thenReturn(imports);

    List<InsightFeedback> ratings = new ArrayList<>();
    for (int index = 0; index < 7; index++) ratings.add(feedback(user, true, "POST_LOSS_OVERTRADING", "STRONG", null));
    ratings.add(feedback(user, false, "POST_LOSS_OVERTRADING", "STRONG", "WRONG_CONCLUSION"));
    ratings.add(feedback(user, false, "OVERTRADING", "MODERATE", "MISSING_CONTEXT"));
    ratings.add(feedback(user, false, "OVERTRADING", "MODERATE", "WRONG_TRADES"));
    ratings.add(feedback(admin, false, "ADMIN_ONLY", "WEAK", "OTHER"));
    ratings.add(feedback(excluded, false, "EXCLUDED_ONLY", "WEAK", "OTHER"));
    when(feedback.findAll()).thenReturn(ratings);

    DataTrustQualityResponse result = new AdminDataTrustService(reconciliations, feedback).quality();

    assertThat(result.importTrust().total()).isEqualTo(12);
    assertThat(result.importTrust().reviewed()).isEqualTo(10);
    assertThat(result.importTrust().matched()).isEqualTo(8);
    assertThat(result.importTrust().mismatched()).isEqualTo(2);
    assertThat(result.importTrust().notReviewed()).isEqualTo(2);
    assertThat(result.importTrust().userConfirmedMatchRate()).isEqualTo(80.0);
    assertThat(result.importTrust().bySource()).containsExactly(
        new DataTrustQualityResponse.RateRow("TOPSTEP", 1, 0, 1, 0.0),
        new DataTrustQualityResponse.RateRow("TRADOVATE", 9, 8, 1, 88.89));
    assertThat(result.importTrust().mismatchReasons()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("PNL_MISMATCH", 2L, "TRADE_COUNT_MISMATCH", 1L));

    assertThat(result.insightTrust().rated()).isEqualTo(10);
    assertThat(result.insightTrust().positive()).isEqualTo(7);
    assertThat(result.insightTrust().negative()).isEqualTo(3);
    assertThat(result.insightTrust().positiveRate()).isEqualTo(70.0);
    assertThat(result.insightTrust().byFindingType()).containsExactly(
        new DataTrustQualityResponse.RateRow("OVERTRADING", 2, 0, 2, 0.0),
        new DataTrustQualityResponse.RateRow("POST_LOSS_OVERTRADING", 8, 7, 1, 87.5));
    assertThat(result.insightTrust().byEvidenceStrength()).containsExactly(
        new DataTrustQualityResponse.RateRow("MODERATE", 2, 0, 2, 0.0),
        new DataTrustQualityResponse.RateRow("STRONG", 8, 7, 1, 87.5));
    assertThat(result.insightTrust().byVersion()).containsExactly(
        new DataTrustQualityResponse.RateRow("insight-trust-v1 / rule-v1", 10, 7, 3, 70.0));
    assertThat(result.insightTrust().negativeReasons()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("WRONG_CONCLUSION", 1L, "MISSING_CONTEXT", 1L, "WRONG_TRADES", 1L));
  }

  @Test void returnsNullRatesForNoFeedback() {
    ImportReconciliationRepository reconciliations = mock(ImportReconciliationRepository.class);
    InsightFeedbackRepository feedback = mock(InsightFeedbackRepository.class);
    when(reconciliations.findAll()).thenReturn(List.of());
    when(feedback.findAll()).thenReturn(List.of());
    DataTrustQualityResponse result = new AdminDataTrustService(reconciliations, feedback).quality();
    assertThat(result.importTrust().userConfirmedMatchRate()).isNull();
    assertThat(result.insightTrust().positiveRate()).isNull();
  }

  private User user(UserRole role, boolean excluded) { User value = new User(); value.setRole(role); value.setAnalyticsExcluded(excluded); return value; }
  private ImportReconciliation reconciliation(User user, ImportReconciliationStatus status, String source, String reasons) { ImportReconciliation value = new ImportReconciliation(); value.setUser(user); value.setStatus(status); value.setSource(source); value.setMismatchReasons(reasons); return value; }
  private InsightFeedback feedback(User user, boolean helpful, String type, String evidence, String reason) { InsightFeedback value = new InsightFeedback(); value.setUser(user); value.setHelpful(helpful); value.setFindingType(type); value.setEvidenceStrength(evidence); value.setEngineVersion("insight-trust-v1"); value.setRuleVersion("rule-v1"); if (!helpful) value.setReason(InsightFeedbackReason.valueOf(reason)); return value; }
}
