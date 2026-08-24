package com.example.memo;

import com.example.memo.domain.Memo;
import com.example.memo.domain.User;
import com.example.memo.repository.MemoRepository;
import com.example.memo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 샘플 사용자와 메모를 생성합니다.
 * 테스트 계정: 재현/1234, 민수/1234
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MemoRepository memoRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.memoRepository = memoRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // 테스트 사용자 생성
        User jaehyeon = userRepository.save(new User("재현", passwordEncoder.encode("1234")));
        User minsu = userRepository.save(new User("민수", passwordEncoder.encode("1234")));

        // 샘플 메모 생성
        memoRepository.save(new Memo(
                "스프링 공부 시작",
                "Controller - Service - Repository 3계층 구조부터 이해하기.",
                jaehyeon));

        memoRepository.save(new Memo(
                "JPA 더티 체킹",
                "트랜잭션 안에서 조회한 엔티티는 값만 바꿔도 UPDATE 가 나간다.",
                jaehyeon));

        memoRepository.save(new Memo(
                "장보기 목록",
                "우유, 계란, 커피 원두",
                minsu));
    }
}
