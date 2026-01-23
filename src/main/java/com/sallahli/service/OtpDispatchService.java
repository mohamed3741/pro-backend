package com.sallahli.service;

import com.sallahli.model.Enum.DeliveryMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpDispatchService {

    private final SmsService smsService;
    private final WhatsappService whatsappService;

    public void sendVerificationCode(String verificationCode, String phoneNumber, DeliveryMethod deliveryMethod) {
        String message = "Your verification code is: " + verificationCode;

        switch (deliveryMethod) {
            case SMS -> smsService.send(message, phoneNumber);
            case WHATSAPP -> whatsappService.sendOTP(verificationCode, phoneNumber);
            case EMAIL -> {
                // TODO: Implement email service
                log.warn("Email delivery not implemented yet for phone: {}", phoneNumber);
            }
            default -> throw new IllegalArgumentException("Unsupported delivery method: " + deliveryMethod);
        }

        log.info("Verification code sent to {} via {}", phoneNumber, deliveryMethod);
    }
}
