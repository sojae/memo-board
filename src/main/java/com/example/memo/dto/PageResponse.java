package com.example.memo.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 목록 조회 응답. 페이징 정보를 같이 내려줍니다.
 *
 * 스프링의 Page 객체를 그대로 JSON 으로 내보내면
 * 내부 구현이 그대로 노출되고 스프링 버전이 바뀔 때 응답 형태도 바뀝니다.
 * 그래서 필요한 값만 골라 담은 우리 DTO 로 한 번 감싸줍니다.
 *
 *   { "content": [...], "page": 0, "size": 10, "totalElements": 23, "totalPages": 3, "last": false }
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
