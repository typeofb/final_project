package com.mjc.groupware;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class EmailTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void sendEmailTest() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("kstitbiz@gmail.com");
        message.setSubject("Spring Boot 메일 발송 테스트");
        message.setText("지메일 SMTP 연동이 성공적으로 완료되었습니다!");

        mailSender.send(message);
        System.out.println("메일 발송 완료!");
    }
}
