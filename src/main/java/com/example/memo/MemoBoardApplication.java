package com.example.memo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 프로그램의 시작점입니다. (index.js / main.ts 라고 생각하면 됩니다)
 *
 * @SpringBootApplication 이 붙은 클래스를 기준으로,
 * 스프링은 "같은 패키지와 그 하위 패키지"를 전부 스캔해서
 * @RestController, @Service, @Repository 등이 붙은 클래스를 찾아
 * 자동으로 객체를 만들어 관리합니다.
 *
 * 이렇게 스프링이 대신 만들어서 필요한 곳에 넣어주는 것을
 * DI(의존성 주입) 라고 부릅니다. 이 프로젝트에서 제일 중요한 개념입니다.
 */
@SpringBootApplication
public class MemoBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoBoardApplication.class, args);
    }
}
