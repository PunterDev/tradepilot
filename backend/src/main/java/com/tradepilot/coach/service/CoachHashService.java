package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class CoachHashService {

    private final ObjectMapper objectMapper;

    public String hash(CoachDeterministicSummary summary) {
        try {
            String json = objectMapper.writeValueAsString(summary);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return toHex(encodedHash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate coach review hash", e);
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize coach object", e);
        }
    }

    private String toHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            String hexValue = Integer.toHexString(0xff & b);
            if (hexValue.length() == 1) {
                hex.append('0');
            }
            hex.append(hexValue);
        }

        return hex.toString();
    }
}