package com.sallahli.controller;

import com.sallahli.model.Notification;
import com.sallahli.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Push Notification", description = "Push notification management APIs")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/send-to-pro/{proId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Send push notification to pro")
    public ResponseEntity<Void> sendToPro(@PathVariable Long proId, @RequestBody Notification notification) {
        pushNotificationService.sendToPro(proId, notification);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-to-client/{clientId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Send push notification to client")
    public ResponseEntity<Void> sendToClient(@PathVariable Long clientId, @RequestBody Notification notification) {
        pushNotificationService.sendToClient(clientId, notification);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-to-topic/{topic}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Send push notification to topic")
    public ResponseEntity<Void> sendToTopic(@PathVariable String topic, @RequestBody Notification notification) {
        pushNotificationService.sendToTopic(topic, notification);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe device to topic")
    public ResponseEntity<Void> subscribeToTopic(@RequestParam String token, @RequestParam String topic) {
        pushNotificationService.subscribeToTopic(token, topic);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe device from topic")
    public ResponseEntity<Void> unsubscribeFromTopic(@RequestParam String token, @RequestParam String topic) {
        pushNotificationService.unsubscribeFromTopic(token, topic);
        return ResponseEntity.ok().build();
    }
}
