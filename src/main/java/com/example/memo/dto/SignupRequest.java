package com.example.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 2, max = 30, message = "사용자명은 2~30자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 100, message = "비밀번호는 4자 이상이어야 합니다.")
        String password
) {
}
