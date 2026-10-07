package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.InsightFeedbackRequest;
import com.tradepilot.datatrust.dto.InsightFeedbackResponse;
import com.tradepilot.exception.GlobalExceptionHandler;
import com.tradepilot.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class InsightFeedbackControllerTests {
  private DataTrustService service;
  private MockMvc mvc;

  @BeforeEach void setUp() {
    service = mock(DataTrustService.class);
    mvc = standaloneSetup(new DataTrustController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test void getsAndUpdatesEditableFeedbackOnly() throws Exception {
    when(service.feedback(any(), eq(9L), eq("POST_LOSS_OVERTRADING"))).thenReturn(response(true, null));
    when(service.updateFeedback(any(), eq(9L), eq("POST_LOSS_OVERTRADING"), any())).thenReturn(response(false, InsightFeedbackReason.WRONG_CONCLUSION));
    mvc.perform(get("/api/data-trust/insights/9/feedback").param("findingType", "POST_LOSS_OVERTRADING"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.snapshotId").value(9)).andExpect(jsonPath("$.helpful").value(true));
    mvc.perform(put("/api/data-trust/insights/9/feedback").param("findingType", "POST_LOSS_OVERTRADING").contentType(MediaType.APPLICATION_JSON)
            .content("{\"helpful\":false,\"reason\":\"WRONG_CONCLUSION\",\"comment\":\"Optional context\",\"userId\":999,\"accountId\":999,\"findingType\":\"FORGED\",\"engineVersion\":\"forged\",\"ruleVersion\":\"forged\",\"evidenceStrength\":\"WEAK\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.helpful").value(false)).andExpect(jsonPath("$.reason").value("WRONG_CONCLUSION"));
    ArgumentCaptor<InsightFeedbackRequest> request = ArgumentCaptor.forClass(InsightFeedbackRequest.class);
    verify(service).updateFeedback(any(), eq(9L), eq("POST_LOSS_OVERTRADING"), request.capture());
    assertThat(request.getValue().helpful()).isFalse();
    assertThat(request.getValue().reason()).isEqualTo(InsightFeedbackReason.WRONG_CONCLUSION);
    assertThat(request.getValue().comment()).isEqualTo("Optional context");
  }

  @Test void acceptsPositiveAndRejectsInvalidReason() throws Exception {
    when(service.updateFeedback(any(), eq(9L), eq("POST_LOSS_OVERTRADING"), any())).thenReturn(response(true, null));
    mvc.perform(put("/api/data-trust/insights/9/feedback").param("findingType", "POST_LOSS_OVERTRADING").contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":true}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.helpful").value(true));
    mvc.perform(put("/api/data-trust/insights/9/feedback").param("findingType", "POST_LOSS_OVERTRADING").contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":false,\"reason\":\"NOT_A_REASON\"}"))
        .andExpect(status().is4xxClientError());
  }

  @Test void missingLegacyAndCrossUserSnapshotsHaveSafeBodies() throws Exception {
    when(service.feedback(any(), anyLong(), anyString())).thenThrow(new ResourceNotFoundException("Insight not found"));
    for (long id : new long[] {404L, 405L, 406L}) {
      mvc.perform(get("/api/data-trust/insights/{id}/feedback", id).param("findingType", "POST_LOSS_OVERTRADING"))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.findingType").doesNotExist()).andExpect(jsonPath("$.engineVersion").doesNotExist())
          .andExpect(jsonPath("$.ruleVersion").doesNotExist()).andExpect(jsonPath("$.evidenceStrength").doesNotExist())
          .andExpect(jsonPath("$.helpful").doesNotExist());
    }
  }

  private InsightFeedbackResponse response(boolean helpful, InsightFeedbackReason reason) { return new InsightFeedbackResponse(9L, "POST_LOSS_OVERTRADING", helpful, reason, null); }
}
