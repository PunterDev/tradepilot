package com.tradepilot.dto.auth;

import lombok.*;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String name;
    private String email;
}
