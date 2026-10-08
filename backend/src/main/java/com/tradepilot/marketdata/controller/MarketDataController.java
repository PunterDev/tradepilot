package com.tradepilot.marketdata.controller;

import com.tradepilot.marketdata.dto.MarketDataCoverageResponse;
import com.tradepilot.marketdata.dto.MarketDataDownloadRequest;
import com.tradepilot.marketdata.dto.MarketDataDownloadResponse;
import com.tradepilot.marketdata.dto.MarketDataEstimateRequest;
import com.tradepilot.marketdata.dto.MarketDataEstimateResponse;
import com.tradepilot.marketdata.service.HistoricalMarketDataService;
import com.tradepilot.marketdata.service.MarketDataCoverageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/market-data", "/api/v1/admin/market-data"})
@PreAuthorize("hasRole('ADMIN')")
public class MarketDataController {
    private final HistoricalMarketDataService historicalMarketDataService;
    private final MarketDataCoverageService marketDataCoverageService;

    public MarketDataController(HistoricalMarketDataService historicalMarketDataService, MarketDataCoverageService marketDataCoverageService) {
        this.historicalMarketDataService = historicalMarketDataService;
        this.marketDataCoverageService = marketDataCoverageService;
    }

    @PostMapping("/estimate")
    public MarketDataEstimateResponse estimate(@Valid @RequestBody MarketDataEstimateRequest request) {
        return historicalMarketDataService.estimate(request);
    }

    @PostMapping("/download")
    public MarketDataDownloadResponse download(@Valid @RequestBody MarketDataDownloadRequest request) {
        return historicalMarketDataService.download(request);
    }

    @GetMapping("/coverage")
    public List<MarketDataCoverageResponse> coverage() {
        return marketDataCoverageService.coverage();
    }
}
