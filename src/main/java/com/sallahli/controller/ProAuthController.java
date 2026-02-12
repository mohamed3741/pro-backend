package com.sallahli.controller;

import com.sallahli.dto.UserCode;
import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.service.ProAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/pro")
@RequiredArgsConstructor
@Tag(name = "Pro Authentication", description = "APIs for professional registration and authentication")
public class ProAuthController {

    private final ProAuthService proAuthService;

    @PostMapping("/signup")
    @Operation(summary = "Pro Signup", description = "Register a new professional account via Keycloak")
    public ResponseEntity<ProDTO> signup(@RequestBody ProDTO proDTO) {
        try {
            ProDTO result = proAuthService.signup(proDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/check-code")
    @Operation(summary = "Check OTP code", description = "Verify the OTP code sent to the professional")
    public ResponseEntity<Boolean> checkCode(@RequestBody UserCode userCode) {
        try {
            Boolean result = proAuthService.checkCode(userCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/generate-code")
    @Operation(summary = "Generate OTP code", description = "Generate and send a verification code to the professional")
    public ResponseEntity<Boolean> generateCode(
            @RequestParam String username,
            @RequestParam(defaultValue = "WHATSAPP") DeliveryMethod deliveryMethod) {
        try {
            Boolean result = proAuthService.generateVerificationCode(username, deliveryMethod);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/set-phone-number/{tel}")
    @Operation(summary = "Set phone number", description = "Update the professional's phone number")
    public ResponseEntity<ProDTO> registerPhoneNumber(@PathVariable String tel, Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(proAuthService.registerPhoneNumber(tel, username));
    }

    @PreAuthorize("hasAuthority('PRO')")
    @PostMapping("/delete-user")
    @Operation(summary = "Delete account (password)", description = "Delete professional account using password verification")
    public ResponseEntity<String> deleteProfile(Authentication authentication, @RequestBody Map<String, String> body) {
        try {
            proAuthService.deleteUser(authentication, body.get("password"));
            return ResponseEntity.ok("Profile successfully deleted");
        } catch (com.amazonaws.services.apigateway.model.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Incorrect password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error deleting profile");
        }
    }

    @PreAuthorize("hasAuthority('PRO')")
    @PostMapping("/delete-with-otp")
    @Operation(summary = "Delete account (OTP)", description = "Delete professional account using OTP verification")
    public ResponseEntity<String> deleteWithOtp(Authentication authentication, @RequestBody Map<String, String> body) {
        try {
            proAuthService.deleteUserWithOtp(authentication, body.get("otp"));
            return ResponseEntity.ok("User successfully deleted");
        } catch (com.amazonaws.services.apigateway.model.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Incorrect otp");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error deleting profile");
        }
    }

    @PostMapping("/update-names")
    @Operation(summary = "Update names", description = "Update professional's first and last name")
    public ResponseEntity<ProDTO> updateUserNames(
            @RequestBody ProDTO proDTO,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(proAuthService.updateUserNames(username, proDTO));
    }

    @GetMapping("/exists/{username}")
    @Operation(summary = "Check username exists", description = "Check if a username is already registered")
    public ResponseEntity<Boolean> isUsernameExists(@PathVariable String username) {
        boolean exists = proAuthService.isUserExists(username);
        return ResponseEntity.ok(exists);
    }
}
