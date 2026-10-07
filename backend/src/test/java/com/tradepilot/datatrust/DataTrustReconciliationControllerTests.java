package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.ImportReconciliationResponse;
import com.tradepilot.datatrust.dto.ReconciliationUpdateRequest;
import com.tradepilot.exception.GlobalExceptionHandler;
import com.tradepilot.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DataTrustReconciliationControllerTests {
  private DataTrustService service;
  private MockMvc mvc;

  @BeforeEach void setUp() {
    service = mock(DataTrustService.class);
    mvc = standaloneSetup(new DataTrustController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test void getsAuthoritativeReconciliation() throws Exception {
    when(service.reconciliation(any(), eq(123L))).thenReturn(response(ImportReconciliationStatus.NOT_REVIEWED, Set.of(), null));
    mvc.perform(get("/api/data-trust/imports/123/reconciliation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importId").value(123))
        .andExpect(jsonPath("$.tradeCount").value(32))
        .andExpect(jsonPath("$.netPnl").value(664.30))
        .andExpect(jsonPath("$.status").value("NOT_REVIEWED"));
  }

  @Test void acceptsMatchedAndIgnoresForgedCanonicalFields() throws Exception {
    when(service.updateReconciliation(any(), eq(123L), any())).thenReturn(response(ImportReconciliationStatus.MATCHED, Set.of(), null));
    mvc.perform(put("/api/data-trust/imports/123/reconciliation").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"MATCHED\",\"tradeCount\":999999,\"netPnl\":999999,\"accountId\":999}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MATCHED"));
    ArgumentCaptor<ReconciliationUpdateRequest> request = ArgumentCaptor.forClass(ReconciliationUpdateRequest.class);
    verify(service).updateReconciliation(any(), eq(123L), request.capture());
    assertThat(request.getValue().status()).isEqualTo(ImportReconciliationStatus.MATCHED);
    assertThat(request.getValue().mismatchReasons()).isNull();
    assertThat(request.getValue().comment()).isNull();
  }

  @Test void acceptsMismatchDetails() throws Exception {
    when(service.updateReconciliation(any(), eq(123L), any())).thenReturn(response(ImportReconciliationStatus.MISMATCH_REPORTED, Set.of(ImportMismatchReason.PNL_MISMATCH, ImportMismatchReason.TRADE_COUNT_MISMATCH), "Optional context"));
    mvc.perform(put("/api/data-trust/imports/123/reconciliation").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"MISMATCH_REPORTED\",\"mismatchReasons\":[\"PNL_MISMATCH\",\"TRADE_COUNT_MISMATCH\"],\"comment\":\"Optional context\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MISMATCH_REPORTED"))
        .andExpect(jsonPath("$.mismatchReasons").isArray()).andExpect(jsonPath("$.comment").value("Optional context"));
  }

  @Test void rejectsInvalidMismatchReason() throws Exception {
    mvc.perform(put("/api/data-trust/imports/123/reconciliation").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"MISMATCH_REPORTED\",\"mismatchReasons\":[\"NOT_A_REASON\"]}"))
        .andExpect(status().is4xxClientError());
    verifyNoInteractions(service);
  }

  @Test void missingAndCrossUserResourcesMapToSafeNotFoundBodies() throws Exception {
    when(service.reconciliation(any(), eq(404L))).thenThrow(new ResourceNotFoundException("Import reconciliation not found"));
    when(service.reconciliation(any(), eq(124L))).thenThrow(new ResourceNotFoundException("Import reconciliation not found"));
    for (long id : new long[] {404L, 124L}) {
      mvc.perform(get("/api/data-trust/imports/{id}/reconciliation", id))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.tradeCount").doesNotExist())
          .andExpect(jsonPath("$.netPnl").doesNotExist())
          .andExpect(jsonPath("$.source").doesNotExist())
          .andExpect(jsonPath("$.importId").doesNotExist())
          .andExpect(jsonPath("$.mismatchReasons").doesNotExist());
    }
  }

  private ImportReconciliationResponse response(ImportReconciliationStatus status, Set<ImportMismatchReason> reasons, String comment) {
    return new ImportReconciliationResponse(123L, "TRADOVATE", 32, new BigDecimal("664.30"), status, null, reasons, comment);
  }
}
