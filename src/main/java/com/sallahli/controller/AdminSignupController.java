package com.sallahli.controller;

import com.sallahli.dto.sallahli.AdminDTO;
import com.sallahli.service.AdminSignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Signup", description = "Admin registration APIs")
public class AdminSignupController {

    private final AdminSignupService adminSignupService;

    @PostMapping("/signup")
    @Operation(summary = "Register new admin with role assignment")
    public ResponseEntity<AdminDTO> signup(@RequestBody AdminDTO adminDTO) {
        return ResponseEntity.ok(adminSignupService.signup(adminDTO));
    }
}
