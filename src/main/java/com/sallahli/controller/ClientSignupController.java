package com.sallahli.controller;

import com.sallahli.dto.UserCode;
import com.sallahli.dto.UserDTO;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.service.ClientSignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/client")
@RequiredArgsConstructor
public class ClientSignupController {

    private final ClientSignupService clientSignupService;

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(
            @RequestBody UserDTO userDTO,
            @RequestParam(defaultValue = "WHATSAPP") DeliveryMethod deliveryMethod) {

        try {
            UserDTO result = clientSignupService.signup(userDTO, deliveryMethod);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Boolean> verifyCode(@RequestBody UserCode userCode) {
        try {
            Boolean result = clientSignupService.checkCode(userCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Boolean> resendCode(
            @RequestParam String username,
            @RequestParam(defaultValue = "WHATSAPP") DeliveryMethod deliveryMethod) {

        try {
            Boolean result = clientSignupService.generateVerificationCode(username, deliveryMethod);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }
}
