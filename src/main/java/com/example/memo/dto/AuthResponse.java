package com.example.memo.dto;

public record AuthResponse(
        String token,
        String username
) {
}
