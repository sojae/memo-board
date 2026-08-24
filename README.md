# 메모 게시판 — 프론트엔드 개발자를 위한 스프링 부트 CRUD 입문

자바가 처음이어도 따라올 수 있게, 모든 파일에 한글 주석을 달아둔 학습용 프로젝트입니다.
메모 하나를 만들고, 읽고, 고치고, 지우는 것(CRUD)만 하지만 실무 백엔드의 기본 구조는 그대로 담았습니다.

---

## 1. 실행하기

### 준비물

- **JDK 21** — [Temurin 21](https://adoptium.net/) 설치 후 터미널에서 `java -version` 으로 확인
- **IntelliJ IDEA Community** (무료) — 자바는 IDE 도움 없이 쓰면 많이 힘듭니다. VS Code 말고 이걸 권합니다.

DB는 설치할 필요가 없습니다. H2라는 자바 내장 DB를 메모리에 띄워서 씁니다.

### 실행

IntelliJ에서 `memo-board` 폴더를 **Open** 하면 Gradle이 알아서 라이브러리를 받아옵니다.
그 다음 `MemoBoardApplication.java` 를 열고 왼쪽 초록색 ▶ 버튼을 누르면 끝입니다.

터미널에서 하고 싶다면:

```bash
cd memo-board
gradle wrapper          # 처음 한 번만. gradlew 파일을 만들어 줍니다
./gradlew bootRun       # 서버 실행
```

콘솔에 `Started MemoBoardApplication` 이 뜨면 성공입니다.

### 확인

| 주소 | 설명 |
|---|---|
| http://localhost:8080 | 동작 확인용 화면 (목록/작성/수정/삭제/검색) |
| http://localhost:8080/api/memos | API 응답 JSON 직접 보기 |
| http://localhost:8080/h2-console | DB에 SQL 직접 쳐보기 |

> H2 콘솔에 접속할 때 **JDBC URL** 칸에 `jdbc:h2:mem:memodb;MODE=MySQL;DB_CLOSE_DELAY=-1` 을 붙여넣고
> User Name은 `sa`, 비밀번호는 비워둔 채 Connect 하세요. `SELECT * FROM MEMO;` 를 쳐보면 방금 만든 메모가 보입니다.

서버를 끄면 데이터는 사라집니다. 공부용으로 일부러 그렇게 설정했습니다.

---

## 2. API 명세

| 메서드 | 경로 | 설명 | 성공 응답 |
|---|---|---|---|
| POST | `/api/memos` | 메모 생성 | 201 Created |
| GET | `/api/memos?keyword=&page=0&size=10` | 목록 조회 (검색·페이징) | 200 OK |
| GET | `/api/memos/{id}` | 단건 조회 | 200 OK |
| PUT | `/api/memos/{id}` | 수정 | 200 OK |
| DELETE | `/api/memos/{id}` | 삭제 | 204 No Content |

### curl로 직접 때려보기

```bash
# 생성
curl -X POST http://localhost:8080/api/memos \
  -H "Content-Type: application/json" \
  -d '{"title":"첫 메모","content":"내용입니다","author":"재현"}'

# 목록
curl "http://localhost:8080/api/memos?page=0&size=5"

# 검색
curl "http://localhost:8080/api/memos?keyword=메모"

# 수정
curl -X PUT http://localhost:8080/api/memos/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"바뀐 제목","content":"바뀐 내용"}'

# 삭제
curl -X DELETE http://localhost:8080/api/memos/1

# 검증 실패해 보기 (400 + 어떤 필드가 왜 틀렸는지 응답)
curl -X POST http://localhost:8080/api/memos \
  -H "Content-Type: application/json" \
  -d '{"title":"","content":"내용","author":"재현"}'
```

---

## 3. 프로젝트 구조 — 이게 핵심입니다

```
src/main/java/com/example/memo/
├── MemoBoardApplication.java   ← 시작점 (index.js)
├── DataInitializer.java        ← 시작할 때 샘플 데이터 3개 삽입
│
├── domain/Memo.java            ← ① 엔티티: DB 테이블 = 자바 객체
├── repository/MemoRepository.java ← ② DB 접근 (인터페이스만! 구현은 스프링이)
├── dto/                        ← ③ 요청/응답 전용 그릇
│   ├── MemoCreateRequest.java
│   ├── MemoUpdateRequest.java
│   ├── MemoResponse.java
│   └── PageResponse.java
├── service/MemoService.java    ← ④ 실제 로직 + 트랜잭션
├── controller/MemoController.java ← ⑤ HTTP 입구 (Express router)
└── exception/                  ← 에러를 한 곳에서 처리
    ├── MemoNotFoundException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

### 요청 하나가 흐르는 길

```
브라우저
   │  POST /api/memos  { title, content, author }
   ▼
MemoController          JSON을 DTO로 변환 + @Valid 검증
   │  MemoCreateRequest
   ▼
MemoService             new Memo(...) 만들고 저장 요청, 트랜잭션 시작
   │  Memo (엔티티)
   ▼
MemoRepository          JPA가 INSERT SQL 생성
   │
   ▼
H2 데이터베이스
   │
   ▼ (되돌아오며 MemoResponse로 변환)
브라우저에 JSON 응답
```

**왜 이렇게 나누나요?** 컨트롤러에 다 때려넣어도 지금은 돌아갑니다.
하지만 계층을 나눠두면 로직을 다른 입구(배치, 관리자 API)에서 재사용할 수 있고,
서비스만 따로 테스트할 수 있고, DB를 바꿔도 서비스 코드는 안 바뀝니다.
자바 진영에서 이 3계층 구조는 거의 표준이라 실무 코드를 읽을 때도 바로 이 구조가 보입니다.

---

## 4. 프론트엔드 개념과 짝지어 보기

| 익숙한 것 | 스프링에서는 |
|---|---|
| `package.json` | `build.gradle` |
| `npm install` | Gradle이 자동으로 (IDE가 알아서) |
| `.env` / 서버 설정 | `application.yml` |
| Express `router` | `@RestController` + `@GetMapping` 등 |
| `req.body` | `@RequestBody` |
| `req.params.id` | `@PathVariable Long id` |
| `req.query.page` | `@RequestParam int page` |
| `res.json(obj)` | 그냥 객체를 `return` (자동 JSON 변환) |
| `res.status(201)` | `ResponseEntity.status(...)` |
| 에러 미들웨어 | `@RestControllerAdvice` |
| zod / yup 스키마 검증 | `@Valid` + `@NotBlank` `@Size` |
| Prisma / TypeORM | JPA (Hibernate) |
| `type MemoResponse = {...}` | `record MemoResponse(...)` |
| `import { x } from './x'` | `import com.example...` (패키지 = 폴더 경로) |

---

## 5. 처음에 헷갈리는 자바/스프링 개념 5가지

**① `@` 어노테이션이 뭔가요**
클래스나 메서드에 붙이는 메모지입니다. 그 자체로는 아무 일도 안 하고,
스프링이 실행될 때 이걸 읽어서 "아 이건 컨트롤러구나" 하고 처리합니다.
데코레이터(`@Component` in Angular)와 거의 같은 감각입니다.

**② DI(의존성 주입)**
`MemoService` 안에서 `new MemoRepository()` 를 한 번도 안 합니다.
생성자에 파라미터로 적어두기만 하면 스프링이 객체를 만들어서 넣어줍니다.

```java
public MemoService(MemoRepository memoRepository) {  // ← 스프링이 넣어줌
    this.memoRepository = memoRepository;
}
```

덕분에 테스트할 때 가짜 Repository로 바꿔 끼우기가 쉬워집니다.

**③ 엔티티 vs DTO — 왜 둘 다 필요한가**
엔티티는 DB 모양, DTO는 API 모양입니다. 이걸 하나로 합치면
클라이언트가 `id`나 `createdAt`을 마음대로 밀어넣을 수 있고,
DB 컬럼 하나 바꾸면 API 스펙이 같이 깨집니다. 귀찮아 보여도 나중에 이게 살립니다.

**④ 더티 체킹 (`MemoService.update()` 보세요)**
`@Transactional` 안에서 조회한 엔티티는 JPA가 계속 지켜보고 있다가,
값이 바뀌면 메서드가 끝날 때 알아서 UPDATE 쿼리를 날립니다.
그래서 수정할 때 `save()`를 부르지 않습니다. 처음엔 "왜 저장이 되지?" 싶은 부분입니다.

**⑤ `Optional`**
`findById()`는 `Memo`가 아니라 `Optional<Memo>`를 반환합니다.
"값이 없을 수도 있음"을 타입으로 강제하는 장치라, null 체크를 깜빡할 수 없게 만듭니다.
`.orElseThrow(...)` 로 "없으면 예외" 를 한 줄에 표현합니다.

---

## 6. 테스트 돌려보기

```bash
./gradlew test
```

`src/test/java/com/example/memo/MemoControllerTest.java` 에 5개의 테스트가 있습니다.
생성 → 조회 → 수정 → 삭제 → 삭제 후 404 까지 전 흐름을 검증합니다.

MockMvc는 실제로 포트를 열지 않고 "HTTP 요청이 온 척" 해서 전 구간을 돌려봅니다.
백엔드에서는 매번 손으로 눌러보는 것보다 이게 훨씬 빠릅니다.

---

## 7. 추천 학습 순서

이 프로젝트를 그냥 읽지 말고, 아래 순서로 **직접 고쳐보면서** 익히시는 걸 권합니다.

**1주차 — 구조 이해**

1. 서버를 띄우고 http://localhost:8080 에서 CRUD를 다 해봅니다. 하단 "요청 기록"에서 어떤 HTTP 호출이 나가는지 봅니다.
2. `MemoController` → `MemoService` → `MemoRepository` 순서로 코드를 따라 읽습니다.
3. 콘솔에 찍히는 SQL을 봅니다. 내가 짠 자바 코드가 어떤 SQL이 되는지 감을 잡는 게 중요합니다.
4. H2 콘솔에서 `SELECT * FROM MEMO;` 를 직접 쳐봅니다.

**2주차 — 손으로 고치기**

5. `Memo` 엔티티에 `viewCount`(조회수) 필드를 추가하고, 단건 조회할 때마다 1씩 올라가게 만들어 보세요.
6. `MemoRepository`에 `findByAuthor(String author)` 쿼리 메서드를 추가하고 `?author=` 검색을 붙여보세요.
7. 정렬 기준을 쿼리 파라미터로 받게 바꿔보세요. (`?sort=createdAt,desc`)
8. 테스트를 하나 직접 작성해 보세요.

**3주차 — 게시판으로 확장**

9. **댓글 기능** — `Comment` 엔티티를 만들고 `@ManyToOne`으로 메모와 연결합니다. JPA 연관관계가 백엔드 학습의 진짜 고비이자 핵심입니다.
10. **DB 교체** — H2 대신 MySQL이나 PostgreSQL을 Docker로 띄우고 `application.yml`만 바꿔보세요. 코드가 거의 안 바뀌는 걸 확인하면 JPA의 가치가 이해됩니다.
11. **로그인** — Spring Security + JWT. 난이도가 확 뜁니다. 위 10단계까지 편해진 다음에 하세요.
12. **프론트 붙이기** — React/Vue로 이 API를 호출하는 화면을 만들어 보세요. 이미 CORS는 열려 있습니다.

**참고 자료**

- [스프링 공식 가이드 — REST 서비스 만들기](https://spring.io/guides/gs/rest-service)
- [Baeldung](https://www.baeldung.com/) — 스프링 관련 뭘 검색하든 여기가 제일 정확합니다
- [start.spring.io](https://start.spring.io) — 다음 프로젝트를 처음부터 만들 때 쓰는 생성기

---

## 8. 일부러 넣지 않은 것들

공부 초기에 "마법"이 많으면 오히려 이해를 방해해서 뺐습니다.

- **Lombok** — `@Getter`, `@Builder` 로 코드가 확 짧아지지만, 자바 기본 문법에 익숙해진 뒤에 쓰세요. 지금은 getter가 눈에 보이는 게 낫습니다.
- **Spring Security** — 개념이 무겁습니다. CRUD가 손에 익은 다음에.
- **Swagger** — API 문서 자동 생성. 편하지만 지금은 curl로 직접 때려보는 편이 남는 게 많습니다.
- **QueryDSL** — 복잡한 동적 쿼리가 필요해질 때 도입하면 됩니다.

---

## 9. 자주 만나는 에러

| 증상 | 원인과 해결 |
|---|---|
| `Web server failed to start. Port 8080 was already in use` | 다른 프로그램이 8080을 쓰는 중. `application.yml`의 `server.port`를 8081로 바꾸세요. |
| `Table "MEMO" not found` | `ddl-auto` 설정이 꺼졌거나 H2 콘솔의 JDBC URL이 다릅니다. URL을 정확히 복사했는지 확인하세요. |
| 400인데 이유를 모르겠음 | 응답 바디의 `fieldErrors`를 보세요. 어떤 필드가 왜 틀렸는지 나옵니다. |
| 프론트에서 CORS 에러 | `MemoController`의 `@CrossOrigin(origins = "*")` 확인. 그래도 안 되면 프론트 개발 서버 프록시 설정을 보세요. |
| `Unsupported class file major version` | JDK 버전 불일치. IntelliJ의 Project SDK가 21인지 확인하세요. |
