package com.example.memo.repository;

import com.example.memo.domain.Memo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ============================================================
 * 2단계: 레포지토리 (Repository) = DB에 접근하는 계층
 * ============================================================
 *
 * 여기서 제일 놀라운 점: 구현체를 우리가 안 만듭니다.
 * 인터페이스만 선언해두면 스프링이 실행 시점에 구현 클래스를 자동으로 만들어 줍니다.
 *
 * JpaRepository<Memo, Long> 을 상속하면 아래 메서드들이 공짜로 생깁니다.
 *   save(memo)        -> INSERT 또는 UPDATE
 *   findById(id)      -> SELECT ... WHERE id = ?   (Optional 로 감싸서 반환)
 *   findAll(pageable) -> SELECT ... LIMIT ? OFFSET ?
 *   delete(memo)      -> DELETE
 *   existsById(id)    -> 존재 여부만 확인
 *   count()           -> 전체 개수
 */
public interface MemoRepository extends JpaRepository<Memo, Long> {

    /**
     * 이건 "쿼리 메서드" 라는 기능입니다.
     * 메서드 이름을 규칙에 맞게 지으면 스프링이 이름을 해석해서 SQL 을 만들어 줍니다.
     *
     * findBy + Title + Containing + IgnoreCase
     *   -> SELECT * FROM memo WHERE LOWER(title) LIKE LOWER('%키워드%')
     *
     * 이름만 바꿔도 쿼리가 바뀝니다. 예를 들어
     *   findByAuthor(String author)
     *   findByTitleContainingOrContentContaining(String t, String c)
     * 같은 식으로 마음껏 추가해 보세요.
     */
    Page<Memo> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
