package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class AiTradeAnalysisHashService {
    private final ObjectMapper objectMapper;

    public String hash(AiTradeAnalysisRequest request, byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(objectMapper.writeValueAsBytes(request));
            if (imageBytes != null && imageBytes.length > 0) {
                digest.update("|image|".getBytes(StandardCharsets.UTF_8));
                digest.update(imageBytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash AI trade analysis request", e);
        }
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisHashService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
