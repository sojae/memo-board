package com.example.memo.controller;

import com.example.memo.dto.MemoCreateRequest;
import com.example.memo.dto.MemoResponse;
import com.example.memo.dto.MemoUpdateRequest;
import com.example.memo.dto.PageResponse;
import com.example.memo.service.MemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * ============================================================
 * 5단계: 컨트롤러 (Controller) = HTTP 요청을 받는 입구
 * ============================================================
 *
 * Express 의 router 파일이라고 생각하면 정확합니다.
 *
 * @RestController = @Controller + @ResponseBody
 *   -> 메서드가 반환한 자바 객체를 스프링이 자동으로 JSON 으로 바꿔서 응답 바디에 넣어줍니다.
 *      (res.json(...) 을 매번 안 써도 되는 셈입니다)
 *
 * @RequestMapping("/api/memos") = 이 컨트롤러의 모든 경로 앞에 붙는 공통 접두사
 *
 * @CrossOrigin = CORS 허용. 프론트를 localhost:5173(Vite) 같은 다른 포트에서 띄울 때 필요합니다.
 *   공부용이라 전체 허용해 뒀지만, 실무에서는 허용할 origin 을 명시해야 합니다.
 *
 * --- 만들어지는 API 목록 ---
 *   POST   /api/memos          메모 생성
 *   GET    /api/memos          목록 조회 (?keyword=&page=&size=)
 *   GET    /api/memos/{id}     단건 조회
 *   PUT    /api/memos/{id}     수정
 *   DELETE /api/memos/{id}     삭제
 */
@Tag(name = "메모", description = "메모 CRUD API")
@RestController
@RequestMapping("/api/memos")
@CrossOrigin(origins = "*")
public class MemoController {

    private final MemoService memoService;

    // 서비스와 마찬가지로 생성자 주입
    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    /**
     * 메모 생성 -> 201 Created
     *
     * @RequestBody : 요청 바디의 JSON 을 MemoCreateRequest 객체로 변환해 줍니다.
     * @Valid       : DTO 에 걸어둔 @NotBlank, @Size 검증을 실행합니다.
     *                실패하면 MethodArgumentNotValidException 이 터지고
     *                GlobalExceptionHandler 가 400 응답으로 바꿔줍니다.
     *
     * 새 리소스를 만들었을 때는 201 과 함께 Location 헤더에 새 리소스 주소를 넣어주는 것이
     * REST 관례입니다.
     */
    @Operation(summary = "메모 생성", description = "새로운 메모를 생성합니다.")
    @ApiResponse(responseCode = "201", description = "메모 생성 성공")
    @PostMapping
    public ResponseEntity<MemoResponse> create(@Valid @RequestBody MemoCreateRequest request) {
        MemoResponse response = memoService.create(request);
        return ResponseEntity
                .created(URI.create("/api/memos/" + response.id()))
                .body(response);
    }

    /**
     * 목록 조회 -> 200 OK
     *
     * @RequestParam : 쿼리스트링(?keyword=회의&page=0&size=10)을 받습니다.
     * required = false + defaultValue 로 값이 없을 때의 기본값을 지정합니다.
     */
    @Operation(summary = "메모 목록 조회", description = "메모 목록을 페이징하여 조회합니다. 키워드 검색과 작성자 필터를 지원합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<PageResponse<MemoResponse>> findAll(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "작성자 필터") @RequestParam(required = false) String author,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(memoService.findAll(keyword, author, page, size));
    }


    /**
     * 단건 조회 -> 200 OK / 없으면 404
     *
     * @PathVariable : URL 경로의 {id} 부분을 꺼내옵니다. (Express 의 req.params.id)
     */
    @Operation(summary = "메모 단건 조회", description = "ID로 메모를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "메모를 찾을 수 없음")
    @GetMapping("/{id}")
    public ResponseEntity<MemoResponse> findById(@Parameter(description = "메모 ID") @PathVariable Long id) {
        return ResponseEntity.ok(memoService.findById(id));
    }

    /**
     * 수정 -> 200 OK
     */
    @Operation(summary = "메모 수정", description = "기존 메모를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "404", description = "메모를 찾을 수 없음")
    @PutMapping("/{id}")
    public ResponseEntity<MemoResponse> update(
            @Parameter(description = "메모 ID") @PathVariable Long id,
            @Valid @RequestBody MemoUpdateRequest request
    ) {
        return ResponseEntity.ok(memoService.update(id, request));
    }

    /**
     * 삭제 -> 204 No Content (성공했지만 돌려줄 바디가 없다는 뜻)
     */
    @Operation(summary = "메모 삭제", description = "메모를 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "메모를 찾을 수 없음")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "메모 ID") @PathVariable Long id) {
        memoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
