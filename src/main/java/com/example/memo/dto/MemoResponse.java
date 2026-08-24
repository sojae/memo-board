package com.example.memo.dto;

import com.example.memo.domain.Memo;

import java.time.LocalDateTime;

public record MemoResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getUser().getUsername(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
