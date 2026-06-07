package com.tradepilot.dto.session;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionMistakeResponse {
    private String type;
    private String title;
    private String description;
    private String severity;
}
