package com.tradepilot.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tradepilot.observability.ObservabilityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ResendEmailClientHttpIntegrationTests {
    private static final String TEST_API_KEY = "local-test-resend-key";

    private HttpServer server;
    private final List<RecordedRequest> requests = new ArrayList<>();
    private final List<MockResponse> responses = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", this::handleEmails);
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsActualResendRequestWithExpectedAuthenticationJsonAndResponse() throws Exception {
        enqueue(200, "{\"id\":\"email_local_123\"}");

        String emailId = localClient(Duration.ofSeconds(1)).sendHtml(
            "member@example.com",
            "Verify your TradePilot account",
            "<strong>Verify</strong>"
        );

        assertThat(emailId).isEqualTo("email_local_123");
        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.path()).isEqualTo("/emails");
            assertThat(request.authorization()).isEqualTo("Bearer " + TEST_API_KEY);
            assertThat(request.contentType()).startsWith("application/json");
            assertThat(request.body()).contains("member@example.com", "Verify your TradePilot account", "<strong>Verify</strong>");
        });
    }

    @Test
    void propagatesRateLimitAndHttpErrors() {
        enqueue(429, "{\"message\":\"rate limited\",\"name\":\"rate_limit_exceeded\"}");
        assertStatus(429);

        enqueue(400, "{\"message\":\"invalid recipient\",\"name\":\"validation_error\"}");
        assertStatus(400);

        enqueue(500, "{\"message\":\"service unavailable\",\"name\":\"internal_server_error\"}");
        assertStatus(500);
    }

    @Test
    void failsForNetworkAndReadTimeoutWithoutContactingResend() {
        int unusedPort = server.getAddress().getPort();
        server.stop(0);
        server = null;

        assertThatThrownBy(() -> clientFor("http://127.0.0.1:" + unusedPort, Duration.ofMillis(100))
            .sendHtml("member@example.com", "subject", "body"))
            .isInstanceOf(RuntimeException.class)
            .satisfies(error -> assertThat(hasCause(error, IOException.class)).isTrue());
    }

    @Test
    void failsClosedWhenTheLocalEndpointTimesOut() {
        enqueueDelayed(200, "{\"id\":\"too-late\"}", 500);

        assertThatThrownBy(() -> localClient(Duration.ofMillis(50))
            .sendHtml("member@example.com", "subject", "body"))
            .isInstanceOf(RuntimeException.class)
            .satisfies(error -> assertThat(hasCause(error, IOException.class)).isTrue());
    }

    @Test
    void registrationAndPasswordResetEmailFlowsUseTheActualResendClient() {
        enqueue(200, "{\"id\":\"email_verify\"}");
        enqueue(200, "{\"id\":\"email_reset\"}");
        EmailService emailService = new EmailService(localClient(Duration.ofSeconds(1)), mock(ObservabilityService.class));

        emailService.sendVerificationEmail("member@example.com", "https://app.example/verify-email?token=verify-token");
        emailService.sendPasswordResetEmail("member@example.com", "https://app.example/reset-password?token=reset-token");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).body()).contains("Verify your TradePilot account", "verify-email?token=verify-token");
        assertThat(requests.get(1).body()).contains("Reset your TradePilot password", "reset-password?token=reset-token");
    }

    private void assertStatus(int expectedStatus) {
        assertThatThrownBy(() -> localClient(Duration.ofSeconds(1))
            .sendHtml("member@example.com", "subject", "body"))
            .isInstanceOf(ResendException.class)
            .satisfies(error -> assertThat(((ResendException) error).getStatusCode()).isEqualTo(expectedStatus));
    }

    private ResendEmailClient localClient(Duration timeout) {
        return clientFor("http://127.0.0.1:" + server.getAddress().getPort(), timeout);
    }

    private ResendEmailClient clientFor(String baseUrl, Duration timeout) {
        ResendEmailClient client = new ResendEmailClient() {
            @Override
            protected Resend createClient(String apiKey) {
                return Resend.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .callTimeout(timeout)
                    .build();
            }
        };
        ReflectionTestUtils.setField(client, "resendApiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(client, "emailFrom", "TradePilot <no-reply@example.com>");
        return client;
    }

    private void enqueue(int status, String body) {
        responses.add(new MockResponse(status, body, 0));
    }

    private void enqueueDelayed(int status, String body, long delayMillis) {
        responses.add(new MockResponse(status, body, delayMillis));
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> expectedType) {
        Throwable current = error;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void handleEmails(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new RecordedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Authorization"),
            exchange.getRequestHeaders().getFirst("Content-Type"),
            body
        ));
        MockResponse response = responses.remove(0);
        if (response.delayMillis() > 0) {
            try {
                Thread.sleep(response.delayMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private record MockResponse(int status, String body, long delayMillis) {
    }

    private record RecordedRequest(String method, String path, String authorization, String contentType, String body) {
    }
}
