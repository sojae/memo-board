package com.example.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 메모 생성 요청 DTO.
 * 작성자(author) 는 JWT 토큰에서 추출하므로 요청에 포함하지 않습니다.
 */
public record MemoCreateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
        String content
) {
}
