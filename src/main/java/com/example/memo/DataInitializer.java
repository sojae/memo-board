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
        User jieun = userRepository.save(new User("지은", passwordEncoder.encode("1234")));

        // 샘플 메모 생성 — 재현
        memoRepository.save(new Memo("스프링 공부 시작",
                "Controller - Service - Repository 3계층 구조부터 이해하기.", jaehyeon));
        memoRepository.save(new Memo("JPA 더티 체킹",
                "트랜잭션 안에서 조회한 엔티티는 값만 바꿔도 UPDATE 가 나간다.", jaehyeon));
        memoRepository.save(new Memo("REST API 설계 원칙",
                "URI는 명사로, HTTP 메서드로 행위를 구분한다. POST=생성, GET=조회, PUT=수정, DELETE=삭제.", jaehyeon));
        memoRepository.save(new Memo("스프링 시큐리티 정리",
                "SecurityFilterChain으로 인증/인가를 설정한다. JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 끼운다.", jaehyeon));
        memoRepository.save(new Memo("Git 브랜치 전략",
                "main → develop → feature/xxx 순서로 분기하고, PR로 머지한다.", jaehyeon));
        memoRepository.save(new Memo("Docker 기초",
                "Dockerfile 작성 → docker build → docker run. 컨테이너는 가벼운 가상환경이다.", jaehyeon));

        // 샘플 메모 생성 — 민수
        memoRepository.save(new Memo("장보기 목록",
                "우유, 계란, 커피 원두, 식빵, 바나나", minsu));
        memoRepository.save(new Memo("운동 계획",
                "월: 가슴/삼두, 수: 등/이두, 금: 하체/어깨. 유산소는 매일 30분.", minsu));
        memoRepository.save(new Memo("독서 목록",
                "1. 클린 코드\n2. 이펙티브 자바\n3. 객체지향의 사실과 오해\n4. 도메인 주도 설계", minsu));
        memoRepository.save(new Memo("여행 준비물",
                "여권, 충전기, 우산, 상비약, 여행자 보험 가입하기", minsu));
        memoRepository.save(new Memo("면접 준비",
                "자기소개 1분 버전 준비, 프로젝트 경험 정리, CS 기본 개념 복습(OS, 네트워크, DB)", minsu));

        // 샘플 메모 생성 — 지은
        memoRepository.save(new Memo("React vs Vue 비교",
                "React: JSX, 자유도 높음, 생태계 큼. Vue: 템플릿 기반, 러닝커브 낮음, 공식 도구 잘 갖춰짐.", jieun));
        memoRepository.save(new Memo("CSS Grid 정리",
                "grid-template-columns, grid-template-rows로 레이아웃 잡기. gap으로 간격 설정. fr 단위 활용.", jieun));
        memoRepository.save(new Memo("TypeScript 타입 가드",
                "typeof, instanceof, in 연산자, 사용자 정의 타입 가드(is 키워드)로 타입을 좁힌다.", jieun));
        memoRepository.save(new Memo("API 에러 핸들링 패턴",
                "try-catch로 감싸고, 에러 코드별 분기 처리. 토스트 메시지로 사용자에게 피드백.", jieun));
        memoRepository.save(new Memo("프로젝트 회고",
                "잘한 점: 일정 준수, 코드 리뷰 활성화. 개선할 점: 테스트 커버리지 부족, 문서화 미흡.", jieun));
        memoRepository.save(new Memo("알고리즘 스터디 노트",
                "이번 주 주제: BFS/DFS. 그래프 탐색은 큐(BFS)와 스택(DFS)의 차이를 이해하면 된다.", jieun));
    }
}
