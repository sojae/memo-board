package com.example.memo.repository;

import com.example.memo.domain.Memo;
import com.example.memo.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * JdbcTemplate 을 사용한 메모 레포지토리
 * ============================================================
 *
 * JPA(MemoRepository) 와 동일한 기능을 SQL 직접 작성 방식으로 구현합니다.
 * JPA 가 자동으로 해주던 것들을 여기서는 전부 손으로 작성합니다:
 *   1) SQL 쿼리 직접 작성
 *   2) ResultSet → 자바 객체 매핑 (RowMapper)
 *   3) 페이징 처리 (LIMIT, OFFSET 직접 계산)
 *   4) INSERT 후 생성된 ID 가져오기 (KeyHolder)
 *
 * 비교해보면 JPA 가 얼마나 많은 걸 대신해주는지 체감할 수 있습니다.
 */
@Repository
public class MemoJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ──────────────────────────────────────────────
    // RowMapper: DB 조회 결과(ResultSet) → Memo 객체로 변환
    // ──────────────────────────────────────────────
    // JPA 에서는 이 작업을 Hibernate 가 자동으로 해줬지만,
    // JDBC 에서는 컬럼 하나하나를 직접 꺼내서 객체에 넣어야 합니다.
    //
    // memo 테이블과 users 테이블을 JOIN 해서 가져오므로
    // User 객체도 함께 만들어야 합니다.
    private final RowMapper<Memo> memoRowMapper = (rs, rowNum) -> {
        // 1) User 객체 생성 - JOIN 으로 가져온 users 테이블 데이터
        User user = new User(
                rs.getString("username"),
                ""  // password 는 조회할 필요 없으므로 빈 문자열
        );
        // User 의 id 는 setter 가 없으므로 리플렉션으로 설정
        // (실제 프로덕션에서는 별도 DTO 를 쓰는 게 일반적)
        setFieldValue(user, "id", rs.getLong("user_id"));
        setFieldValue(user, "createdAt", toLocalDateTime(rs.getTimestamp("user_created_at")));

        // 2) Memo 객체 생성
        Memo memo = new Memo(
                rs.getString("title"),
                rs.getString("content"),
                user
        );
        // id, createdAt, updatedAt 은 생성자에 없으므로 리플렉션으로 설정
        setFieldValue(memo, "id", rs.getLong("id"));
        setFieldValue(memo, "createdAt", toLocalDateTime(rs.getTimestamp("created_at")));
        setFieldValue(memo, "updatedAt", toLocalDateTime(rs.getTimestamp("updated_at")));

        return memo;
    };

    // ──────────────────────────────────────────────
    // 1. save: INSERT (새 메모 저장)
    // ──────────────────────────────────────────────
    // JPA: memoRepository.save(memo) 한 줄이면 끝
    // JDBC: SQL 작성 + 파라미터 바인딩 + KeyHolder 로 생성된 ID 받기
    public Memo save(Memo memo) {
        LocalDateTime now = LocalDateTime.now();

        if (memo.getId() == null) {
            // INSERT
            String sql = "INSERT INTO memo (title, content, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, memo.getTitle());
                ps.setString(2, memo.getContent());
                ps.setLong(3, memo.getUser().getId());
                ps.setTimestamp(4, Timestamp.valueOf(now));
                ps.setTimestamp(5, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);

            Long generatedId = keyHolder.getKey().longValue();
            setFieldValue(memo, "id", generatedId);
            setFieldValue(memo, "createdAt", now);
            setFieldValue(memo, "updatedAt", now);
        } else {
            // UPDATE
            String sql = "UPDATE memo SET title = ?, content = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql, memo.getTitle(), memo.getContent(), Timestamp.valueOf(now), memo.getId());
            setFieldValue(memo, "updatedAt", now);
        }

        return memo;
    }

    // ──────────────────────────────────────────────
    // 2. findById: SELECT ... WHERE id = ?
    // ──────────────────────────────────────────────
    // JPA: memoRepository.findById(id) → Optional<Memo>
    // JDBC: SQL 직접 작성 + JOIN + 결과 없으면 Optional.empty()
    public Optional<Memo> findById(Long id) {
        String sql = """
                SELECT m.id, m.title, m.content, m.user_id, m.created_at, m.updated_at,
                       u.username, u.created_at AS user_created_at
                FROM memo m
                JOIN users u ON m.user_id = u.id
                WHERE m.id = ?
                """;

        List<Memo> results = jdbcTemplate.query(sql, memoRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // ──────────────────────────────────────────────
    // 3. findAll (페이징): SELECT ... ORDER BY ... LIMIT ? OFFSET ?
    // ──────────────────────────────────────────────
    // JPA: memoRepository.findAll(pageable) → Page<Memo> (페이징 자동)
    // JDBC: LIMIT/OFFSET 직접 계산 + COUNT 쿼리 별도 실행 + PageImpl 로 감싸기
    public Page<Memo> findAll(Pageable pageable) {
        String sql = """
                SELECT m.id, m.title, m.content, m.user_id, m.created_at, m.updated_at,
                       u.username, u.created_at AS user_created_at
                FROM memo m
                JOIN users u ON m.user_id = u.id
                ORDER BY m.created_at DESC
                LIMIT ? OFFSET ?
                """;

        List<Memo> memos = jdbcTemplate.query(sql, memoRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        // 전체 개수를 별도 쿼리로 조회 (페이징 정보에 필요)
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memo", Long.class);

        return new PageImpl<>(memos, pageable, total);
    }

    // ──────────────────────────────────────────────
    // 4. findByTitleContainingIgnoreCase (키워드 검색 + 페이징)
    // ──────────────────────────────────────────────
    // JPA: 메서드 이름만 쓰면 끝
    // JDBC: LIKE 쿼리 + LOWER() 함수 직접 작성
    public Page<Memo> findByTitleContainingIgnoreCase(String keyword, Pageable pageable) {
        String sql = """
                SELECT m.id, m.title, m.content, m.user_id, m.created_at, m.updated_at,
                       u.username, u.created_at AS user_created_at
                FROM memo m
                JOIN users u ON m.user_id = u.id
                WHERE LOWER(m.title) LIKE LOWER(?)
                ORDER BY m.created_at DESC
                LIMIT ? OFFSET ?
                """;

        String pattern = "%" + keyword + "%";
        List<Memo> memos = jdbcTemplate.query(sql, memoRowMapper,
                pattern, pageable.getPageSize(), pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memo WHERE LOWER(title) LIKE LOWER(?)",
                Long.class, pattern);

        return new PageImpl<>(memos, pageable, total);
    }

    // ──────────────────────────────────────────────
    // 5. findByUser_Username (작성자 필터 + 페이징)
    // ──────────────────────────────────────────────
    public Page<Memo> findByUserUsername(String username, Pageable pageable) {
        String sql = """
                SELECT m.id, m.title, m.content, m.user_id, m.created_at, m.updated_at,
                       u.username, u.created_at AS user_created_at
                FROM memo m
                JOIN users u ON m.user_id = u.id
                WHERE u.username = ?
                ORDER BY m.created_at DESC
                LIMIT ? OFFSET ?
                """;

        List<Memo> memos = jdbcTemplate.query(sql, memoRowMapper,
                username, pageable.getPageSize(), pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memo m JOIN users u ON m.user_id = u.id WHERE u.username = ?",
                Long.class, username);

        return new PageImpl<>(memos, pageable, total);
    }

    // ──────────────────────────────────────────────
    // 6. findByUser_UsernameAndTitleContainingIgnoreCase (작성자 + 키워드)
    // ──────────────────────────────────────────────
    public Page<Memo> findByUserUsernameAndTitleContainingIgnoreCase(String username, String keyword, Pageable pageable) {
        String sql = """
                SELECT m.id, m.title, m.content, m.user_id, m.created_at, m.updated_at,
                       u.username, u.created_at AS user_created_at
                FROM memo m
                JOIN users u ON m.user_id = u.id
                WHERE u.username = ? AND LOWER(m.title) LIKE LOWER(?)
                ORDER BY m.created_at DESC
                LIMIT ? OFFSET ?
                """;

        String pattern = "%" + keyword + "%";
        List<Memo> memos = jdbcTemplate.query(sql, memoRowMapper,
                username, pattern, pageable.getPageSize(), pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memo m JOIN users u ON m.user_id = u.id WHERE u.username = ? AND LOWER(m.title) LIKE LOWER(?)",
                Long.class, username, pattern);

        return new PageImpl<>(memos, pageable, total);
    }

    // ──────────────────────────────────────────────
    // 7. delete: DELETE FROM memo WHERE id = ?
    // ──────────────────────────────────────────────
    // JPA: memoRepository.delete(memo) 한 줄
    // JDBC: SQL 직접 작성
    public void delete(Memo memo) {
        jdbcTemplate.update("DELETE FROM memo WHERE id = ?", memo.getId());
    }

    // ──────────────────────────────────────────────
    // 유틸 메서드
    // ──────────────────────────────────────────────

    /** Timestamp → LocalDateTime 변환 (null 안전) */
    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * 리플렉션으로 private 필드에 값을 설정합니다.
     *
     * Memo, User 엔티티에 id 나 createdAt 의 setter 가 없기 때문에
     * 리플렉션을 사용해야 합니다. JPA/Hibernate 도 내부적으로
     * 이와 비슷한 방식으로 private 필드에 값을 주입합니다.
     *
     * 프로덕션에서는 보통 엔티티 대신 별도의 DTO 를 사용하거나,
     * setter 를 추가하는 방식을 씁니다.
     */
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("필드 설정 실패: " + fieldName, e);
        }
    }
}
