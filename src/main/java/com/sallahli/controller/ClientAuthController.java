package com.sallahli.controller;

import com.amazonaws.services.apigateway.model.BadRequestException;
import com.sallahli.dto.UserCode;
import com.sallahli.dto.UserDTO;
import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.service.ClientAuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/client")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ClientDTO> signup(@RequestBody UserDTO userDTO) {

        try {
            ClientDTO result = clientAuthService.signup(userDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/check-code")
    public ResponseEntity<Boolean> checkCode(@RequestBody UserCode userCode) {
        try {
            Boolean result = clientAuthService.checkCode(userCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }



    @GetMapping("/generate-code")
    public ResponseEntity<Boolean> generateCode(
            @RequestParam String username,
            @RequestParam(defaultValue = "WHATSAPP") DeliveryMethod deliveryMethod) {

        try {
            Boolean result = clientAuthService.generateVerificationCode(username, deliveryMethod);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/set-phone-number/{tel}")
    public ResponseEntity<ClientDTO> registerPhoneNumber(@PathVariable String tel, Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(clientAuthService.registerPhoneNumber(tel, username));
    }

    @PreAuthorize("hasAuthority('CLIENT')")
    @PostMapping("/delete-user")
    public ResponseEntity<String> deleteProfile(Authentication authentication, @RequestBody Map<String, String> body) {
        try {
            clientAuthService.deleteUser(authentication, body.get("password"));
            return ResponseEntity.ok("Profile successfully deleted");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Incorrect password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error deleting profile");
        }
    }

    @PreAuthorize("hasAuthority('CLIENT')")
    @PostMapping("/delete-with-otp")
    public ResponseEntity<String> deleteWithOtp(Authentication authentication, @RequestBody Map<String, String> body) {
        try {
            clientAuthService.deleteUserWithOtp(authentication, body.get("otp"));
            return ResponseEntity.ok("User successfully deleted");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Incorrect otp");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error deleting profile");
        }
    }

    @PostMapping("/update-names")
    @Operation(tags = {"Auth"})
    public ResponseEntity<ClientDTO> updateUserNames(
            @RequestBody UserDTO userDTO,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(clientAuthService.updateUserNames(username, userDTO));
    }


    @GetMapping("/exists/{username}")
    public ResponseEntity<Boolean> isUsernameExists(@PathVariable String username) {
        boolean exists = clientAuthService.isUserExists(username);
        return ResponseEntity.ok(exists);
    }
}
