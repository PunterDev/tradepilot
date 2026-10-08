package com.tradepilot.datatrust;

import com.tradepilot.datatrust.dto.*;
import com.tradepilot.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-trust")
public class DataTrustController {
    private final DataTrustService service;
    public DataTrustController(DataTrustService service){this.service=service;}
    @GetMapping("/imports/{importId}/reconciliation") public ImportReconciliationResponse reconciliation(@AuthenticationPrincipal User user,@PathVariable Long importId){return service.reconciliation(user,importId);}
    @PutMapping("/imports/{importId}/reconciliation") public ImportReconciliationResponse reconciliation(@AuthenticationPrincipal User user,@PathVariable Long importId,@Valid @RequestBody ReconciliationUpdateRequest request){return service.updateReconciliation(user,importId,request);}
    @GetMapping("/insights/{snapshotId}/feedback") public InsightFeedbackResponse feedback(@AuthenticationPrincipal User user,@PathVariable Long snapshotId,@RequestParam String findingType){return service.feedback(user,snapshotId,findingType);}
    @PutMapping("/insights/{snapshotId}/feedback") public InsightFeedbackResponse feedback(@AuthenticationPrincipal User user,@PathVariable Long snapshotId,@RequestParam String findingType,@Valid @RequestBody InsightFeedbackRequest request){return service.updateFeedback(user,snapshotId,findingType,request);}
}
