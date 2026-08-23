package com.tradepilot.tradereplay.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradeReplayScheduler {
    private final TradeReplayService tradeReplayService;

    public TradeReplayScheduler(TradeReplayService tradeReplayService) {
        this.tradeReplayService = tradeReplayService;
    }

    @Scheduled(
            initialDelayString = "${tradepilot.market-data.worker-initial-delay-ms:60000}",
            fixedDelayString = "${tradepilot.market-data.worker-fixed-delay-ms:300000}"
    )
    public void processRunnableReplays() {
        tradeReplayService.processRunnableReplays();
    }
}
