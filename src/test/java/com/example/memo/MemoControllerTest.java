package com.example.memo;

import com.example.memo.domain.User;
import com.example.memo.dto.MemoCreateRequest;
import com.example.memo.dto.MemoUpdateRequest;
import com.example.memo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("테스터")) {
            userRepository.save(new User("테스터", passwordEncoder.encode("1234")));
        }
    }

    @Test
    @DisplayName("메모를 생성하면 201과 함께 저장된 메모가 반환된다")
    @WithMockUser(username = "테스터")
    void createMemo() throws Exception {
        var request = new MemoCreateRequest("테스트 제목", "테스트 내용");

        mockMvc.perform(post("/api/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("테스트 제목"))
                .andExpect(jsonPath("$.author").value("테스터"));
    }

    @Test
    @DisplayName("제목이 비어 있으면 400과 함께 필드 에러 메시지가 반환된다")
    @WithMockUser(username = "테스터")
    void createMemo_validationFail() throws Exception {
        var request = new MemoCreateRequest("", "내용");

        mockMvc.perform(post("/api/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("제목은 필수입니다."));
    }

    @Test
    @DisplayName("목록 조회는 페이징 정보를 함께 반환한다")
    void findAll() throws Exception {
        mockMvc.perform(get("/api/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @DisplayName("생성 -> 조회 -> 수정 -> 삭제 전체 흐름이 동작한다")
    @WithMockUser(username = "테스터")
    void fullCrudFlow() throws Exception {
        // 1. 생성
        var createRequest = new MemoCreateRequest("원본 제목", "원본 내용");
        String createdJson = mockMvc.perform(post("/api/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createdJson).get("id").asLong();

        // 2. 단건 조회
        mockMvc.perform(get("/api/memos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("원본 제목"));

        // 3. 수정
        var updateRequest = new MemoUpdateRequest("바뀐 제목", "바뀐 내용");
        mockMvc.perform(put("/api/memos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.author").value("테스터"));

        // 4. 삭제
        mockMvc.perform(delete("/api/memos/{id}", id))
                .andExpect(status().isNoContent());

        // 5. 삭제 후 조회하면 404
        mockMvc.perform(get("/api/memos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("없는 id 로 조회하면 404가 반환된다")
    void findById_notFound() throws Exception {
        mockMvc.perform(get("/api/memos/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 메모를 생성하면 401이 반환된다")
    void createMemo_unauthorized() throws Exception {
        var request = new MemoCreateRequest("제목", "내용");

        mockMvc.perform(post("/api/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
