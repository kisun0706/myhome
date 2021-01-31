package com.megait.myhome.service;

import com.megait.myhome.util.EmailMessage;
import com.megait.myhome.util.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@Slf4j
public class ConsoleEmailService implements EmailService {
    @Override
    public void sendEmail(EmailMessage emailMessage) {
        log.info("email has sent: {}", emailMessage.getMessage());
    }
}
