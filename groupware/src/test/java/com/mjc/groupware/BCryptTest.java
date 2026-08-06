package com.mjc.groupware;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptTest {

    @Test
    void testGenerateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("HASH 비밀번호 생성: " + encoder.encode("1qaz2wsx!@"));
        System.out.println("HASH 비밀번호 생성: " + encoder.encode("1234"));
    }
}
