package com.tradepilot.manualbacktest.controller;

import com.tradepilot.manualbacktest.service.ManualBacktestService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ManualBacktestControllerRouteTests {
    @Test void versionedCanonicalOrderRouteIsMapped() throws Exception {
        ManualBacktestService service = mock(ManualBacktestService.class);
        MockMvc mvc = standaloneSetup(new ManualBacktestController(service)).build();
        when(service.placeOrder(any(), eq(31L), any())).thenReturn(null);
        mvc.perform(post("/api/v1/manual-backtests/sessions/31/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"LONG\",\"orderType\":\"MARKET\",\"configuredEntryPrice\":null,\"stopPrice\":29331.50,\"targetPrice\":29481.25}"))
                .andExpect(status().isOk());
        verify(service).placeOrder(any(), eq(31L), argThat(order -> order.orderType().name().equals("MARKET") && order.configuredEntryPrice()==null));
    }
}
