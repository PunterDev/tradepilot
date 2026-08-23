package com.tradepilot.playbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlaybookJsonService {
    private final ObjectMapper objectMapper;

    public PlaybookJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize playbook data.", ex);
        }
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public Map<Long, String> readChecklistResults(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, Long.class, String.class));
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
