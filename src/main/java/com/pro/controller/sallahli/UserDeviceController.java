package com.pro.controller.sallahli;

import com.pro.dto.sallahli.request.UserDeviceRegistrationRequest;
import com.pro.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sallahli/devices")
@RequiredArgsConstructor
@Tag(name = "Device Management", description = "APIs for managing user devices for push notifications")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @PostMapping("/register")
    @Operation(summary = "Register a user device for push notifications")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody UserDeviceRegistrationRequest request) {
        userDeviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Unregister a device by token")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String token) {
        userDeviceService.unregisterDevice(token);
        return ResponseEntity.ok().build();
    }
}
