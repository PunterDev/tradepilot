package com.tradepilot.dto.auth;

import lombok.*;

@Data
@Builder
public class UserMeResponse {
    private Long id;
    private String name;
    private String email;
}
