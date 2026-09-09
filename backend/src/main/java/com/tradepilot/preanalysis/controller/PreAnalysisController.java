package com.tradepilot.preanalysis.controller;

import com.tradepilot.entity.User;
import com.tradepilot.preanalysis.dto.PreAnalysisAccessResponse;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.*;
import com.tradepilot.preanalysis.dto.TradingPlaybookDto;
import com.tradepilot.preanalysis.dto.TradingPlaybookRequest;
import com.tradepilot.preanalysis.entity.PreAnalysisMarket;
import com.tradepilot.preanalysis.entity.PreAnalysisTimeframe;
import com.tradepilot.preanalysis.service.PreAnalysisService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping({"/api/pre-analysis", "/api/v1/pre-analysis"})
public class PreAnalysisController {
    private final PreAnalysisService service;

    @GetMapping("/access")
    public PreAnalysisAccessResponse access(@AuthenticationPrincipal User user) {
        return service.access(user);
    }

    @GetMapping("/playbook")
    public TradingPlaybookDto playbook(@AuthenticationPrincipal User user) {
        return service.getActivePlaybook(user);
    }

    @PutMapping("/playbook")
    public TradingPlaybookDto updatePlaybook(@AuthenticationPrincipal User user, @RequestBody TradingPlaybookRequest request) {
        return service.updateActivePlaybook(user, request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(@AuthenticationPrincipal User user,
                                    @RequestParam PreAnalysisMarket market,
                                    @RequestParam PreAnalysisTimeframe timeframe,
                                    @RequestParam(required = false) String traderNote,
                                    @RequestPart("screenshot") MultipartFile screenshot) {
        return service.analyze(user, market, timeframe, traderNote, screenshot);
    }

    @GetMapping("/history")
    public List<AnalysisSummaryResponse> history(@AuthenticationPrincipal User user) {
        return service.history(user);
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal User user, @PathVariable Long id) {
        PreAnalysisService.ImageResource image = service.image(user, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }

    @PostMapping("/{id}/feedback")
    public FeedbackResponse feedback(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody FeedbackRequest request) {
        return service.feedback(user, id, request);
    }

    public PreAnalysisController(PreAnalysisService service) {
        this.service = service;
    }
}
