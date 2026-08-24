package com.example.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 메모 수정 요청 바디입니다.
 * 작성자(author)는 바꿀 수 없게 일부러 뺐습니다.
 * "무엇을 바꿀 수 있는가"를 DTO 필드로 표현하는 것이 포인트입니다.
 */
public record MemoUpdateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
        String content
) {
}
