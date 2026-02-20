package com.sallahli.controller;

import com.sallahli.dto.sallahli.AdminNotificationRequestDTO;
import com.sallahli.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "Back-office APIs for broadcasting and targeted push notifications")
public class AdminNotificationController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Send a push notification to users based on target audience rules")
    public ResponseEntity<Void> sendNotification(@RequestBody AdminNotificationRequestDTO request) {
        pushNotificationService.sendAdminNotification(request);
        return ResponseEntity.ok().build();
    }
}
