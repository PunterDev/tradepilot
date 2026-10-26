package com.tradepilot.survival;

import com.tradepilot.entity.User;
import com.tradepilot.survival.dto.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/accounts/{accountId}/survival")
public class AccountSurvivalController {
 private final AccountSurvivalService service; public AccountSurvivalController(AccountSurvivalService service){this.service=service;}
 @GetMapping("/profile") public AccountRiskProfileResponse profile(@AuthenticationPrincipal User user,@PathVariable Long accountId){return service.profile(user,accountId);}
 @PutMapping("/profile") public AccountRiskProfileResponse profile(@AuthenticationPrincipal User user,@PathVariable Long accountId,@RequestBody AccountRiskProfileRequest request){return service.saveProfile(user,accountId,request);}
 @GetMapping public AccountSurvivalAssessmentResponse assessment(@AuthenticationPrincipal User user,@PathVariable Long accountId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.assessment(user,accountId,date);}
 @GetMapping("/commitment") public ProtectionCommitmentResponse commitment(@AuthenticationPrincipal User user,@PathVariable Long accountId,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.commitment(user,accountId,date);}
 @PutMapping("/commitment") public ProtectionCommitmentResponse commitment(@AuthenticationPrincipal User user,@PathVariable Long accountId,@RequestBody ProtectionCommitmentRequest request){return service.saveCommitment(user,accountId,request);}
 @PostMapping("/commitment/{date}/accept") public ProtectionCommitmentResponse accept(@AuthenticationPrincipal User user,@PathVariable Long accountId,@PathVariable@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.acceptCommitment(user,accountId,date);}
 @PostMapping("/commitment/{date}/evaluate") public CommitmentEvaluationResponse evaluate(@AuthenticationPrincipal User user,@PathVariable Long accountId,@PathVariable@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.evaluateCommitment(user,accountId,date);}
 @GetMapping("/commitment/{date}/evaluation") public CommitmentEvaluationResponse evaluation(@AuthenticationPrincipal User user,@PathVariable Long accountId,@PathVariable@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.currentEvaluation(user,accountId,date);}
}
