package com.sallahli.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    public void send(String message, String phoneNumber) {
        // TODO: Implement actual SMS sending service (Twilio, AWS SNS, etc.)
        log.info("SMS would be sent to {}: {}", phoneNumber, message);
        // For now, just log the message
    }

    public boolean twilioOtpCodeVerification(String phoneNumber, String code) {
        // TODO: Implement Twilio OTP verification
        log.info("Twilio OTP verification for {} with code: {}", phoneNumber, code);
        // For now, always return true for testing
        return true;
    }
}
