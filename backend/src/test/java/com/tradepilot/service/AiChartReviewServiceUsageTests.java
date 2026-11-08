package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiGeneratedResponse;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.ai.openai.OpenAiUnavailableException;
import com.tradepilot.ai.openai.OpenAiUsage;
import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.ai.strategy.StrategyContextService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.entity.User;
import com.tradepilot.security.OwnershipGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChartReviewServiceUsageTests {
    @Test
    void successfulChartReviewUsesTheExistingTradeReviewCreditPath() {
        OwnershipGuard ownership = mock(OwnershipGuard.class);
        TradeService trades = mock(TradeService.class);
        StrategyContextService strategy = mock(StrategyContextService.class);
        OpenAiResponsesClient client = mock(OpenAiResponsesClient.class);
        UsageLimitService usage = mock(UsageLimitService.class);
        User user = new User();
        user.setId(44L);
        when(ownership.requireCurrentUserId(user)).thenReturn(44L);
        when(strategy.resolve(44L, null, "note")).thenReturn(StrategyContext.general());
        when(client.generateJson(anyLong(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new OpenAiGeneratedResponse("{\"grade\":\"B\",\"summary\":\"Visible chart evidence only\"}", OpenAiUsage.empty()));
        AiChartReviewService service = new AiChartReviewService(new ObjectMapper(), ownership, trades, strategy, client, usage);

        service.reviewChart(user, new MockMultipartFile("file", "chart.png", "image/png", new byte[] {1, 2}), null, "note");

        verify(usage).enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
        verify(usage).recordAiUsage(user, UsageType.AI_TRADE_REVIEW, null);
    }

    @Test
    void failedProviderCallDoesNotRecordACompletedCreditUse() {
        OwnershipGuard ownership = mock(OwnershipGuard.class);
        StrategyContextService strategy = mock(StrategyContextService.class);
        OpenAiResponsesClient client = mock(OpenAiResponsesClient.class);
        UsageLimitService usage = mock(UsageLimitService.class);
        User user = new User();
        user.setId(44L);
        when(ownership.requireCurrentUserId(user)).thenReturn(44L);
        when(strategy.resolve(44L, null, "note")).thenReturn(StrategyContext.general());
        when(client.generateJson(anyLong(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new OpenAiUnavailableException("provider unavailable"));
        AiChartReviewService service = new AiChartReviewService(new ObjectMapper(), ownership, mock(TradeService.class), strategy, client, usage);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reviewChart(user, new MockMultipartFile("file", "chart.png", "image/png", new byte[] {1}), null, "note"))
                .isInstanceOf(OpenAiUnavailableException.class);
        verify(usage).enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
        verify(usage, never()).recordAiUsage(user, UsageType.AI_TRADE_REVIEW, null);
    }

    @Test
    void malformedProviderOutputDoesNotRecordACompletedCreditUse() {
        OwnershipGuard ownership = mock(OwnershipGuard.class);
        StrategyContextService strategy = mock(StrategyContextService.class);
        OpenAiResponsesClient client = mock(OpenAiResponsesClient.class);
        UsageLimitService usage = mock(UsageLimitService.class);
        User user = new User();
        user.setId(44L);
        when(ownership.requireCurrentUserId(user)).thenReturn(44L);
        when(strategy.resolve(44L, null, "note")).thenReturn(StrategyContext.general());
        when(client.generateJson(anyLong(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new OpenAiGeneratedResponse("not-json", OpenAiUsage.empty()));
        AiChartReviewService service = new AiChartReviewService(new ObjectMapper(), ownership, mock(TradeService.class), strategy, client, usage);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reviewChart(user, new MockMultipartFile("file", "chart.png", "image/png", new byte[] {1}), null, "note"))
                .isInstanceOf(OpenAiUnavailableException.class);
        verify(usage).enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
        verify(usage, never()).recordAiUsage(user, UsageType.AI_TRADE_REVIEW, null);
    }
}
