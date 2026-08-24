package com.example.memo.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 에러가 났을 때 내려줄 공통 응답 형태입니다.
 * 에러 응답 모양이 항상 같으면 프론트에서 처리하기가 훨씬 편합니다.
 *
 *   {
 *     "status": 400,
 *     "message": "입력값이 올바르지 않습니다.",
 *     "fieldErrors": { "title": "제목은 필수입니다." },
 *     "timestamp": "2026-08-14T10:00:00"
 *   }
 */
public record ErrorResponse(
        int status,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Map.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, message, fieldErrors, LocalDateTime.now());
    }
}
