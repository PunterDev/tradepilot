package com.tradepilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.seo-events.max-requests=2",
        "app.rate-limit.seo-events.window-seconds=60",
        "app.rate-limit.seo-events.max-content-bytes=512"
})
class SeoAnalyticsRateLimitTests {
    @Autowired MockMvc mockMvc;

    @Test
    void publicSeoEventsAreRateLimitedByAnonymousRequestSubject() throws Exception {
        postEvent("55555555-5555-4555-8555-555555555551", 204);
        postEvent("55555555-5555-4555-8555-555555555552", 204);
        postEvent("55555555-5555-4555-8555-555555555553", 429);
    }

    @Test
    void publicSeoEventsRejectOversizedBodiesBeforeControllerHandling() throws Exception {
        String body = """
                {"events":[{"event":"seo_landing_page_view","landingPage":"/revenge-trading","utmContent":"%s"}]}
                """.formatted("a".repeat(700));

        mockMvc.perform(post("/api/v1/seo/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPayloadTooLarge());
    }

    private void postEvent(String eventId, int expectedStatus) throws Exception {
        String body = """
                {"events":[{"eventId":"%s","event":"seo_landing_page_view","landingPage":"/revenge-trading"}]}
                """.formatted(eventId);
        mockMvc.perform(post("/api/v1/seo/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }
}
