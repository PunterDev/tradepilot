package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.DataTrustQualityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdminDataTrustControllerTests {
  @Test void returnsImportAndInsightQualityContract() throws Exception {
    AdminDataTrustService service = mock(AdminDataTrustService.class);
    when(service.quality()).thenReturn(new DataTrustQualityResponse(
        new DataTrustQualityResponse.ImportTrust(12, 10, 8, 2, 2, 80.0, List.of(new DataTrustQualityResponse.RateRow("TRADOVATE", 9, 8, 1, 88.89)), Map.of("PNL_MISMATCH", 2L)),
        new DataTrustQualityResponse.InsightTrust(10, 7, 3, 70.0, List.of(), List.of(), List.of(), Map.of("WRONG_CONCLUSION", 1L))));
    MockMvc mvc = standaloneSetup(new AdminDataTrustController(service)).build();
    mvc.perform(get("/api/admin/data-trust/quality"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.importTrust.reviewed").value(10))
        .andExpect(jsonPath("$.importTrust.userConfirmedMatchRate").value(80.0))
        .andExpect(jsonPath("$.insightTrust.rated").value(10))
        .andExpect(jsonPath("$.insightTrust.positiveRate").value(70.0));
  }
}
