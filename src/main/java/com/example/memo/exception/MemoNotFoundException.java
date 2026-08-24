package com.example.memo.exception;

/**
 * 메모를 못 찾았을 때 던지는 예외입니다.
 *
 * RuntimeException 을 상속하면 메서드에 throws 를 안 붙여도 됩니다.
 * (자바에는 checked / unchecked 예외 구분이 있는데,
 *  스프링에서는 보통 unchecked = RuntimeException 계열을 씁니다)
 */
public class MemoNotFoundException extends RuntimeException {

    public MemoNotFoundException(Long id) {
        super("메모를 찾을 수 없습니다. id=" + id);
    }
}
