package com.tradepilot.billing;

import com.stripe.exception.SignatureVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    private final StripeBillingService stripeBillingService;

    @PostMapping("/webhook")
    public Map<String, Boolean> webhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader
    ) {
        log.info("Stripe webhook controller reached");
        log.trace("Stripe webhook request received. signaturePresent={}", StringUtils.hasText(signatureHeader));

        if (!StringUtils.hasText(signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        }

        try {
            stripeBillingService.handleWebhook(payload == null ? "" : payload, signatureHeader);
            return Map.of("received", true);
        } catch (SignatureVerificationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook payload");
        }
    }
}
