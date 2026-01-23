package com.pro.controller.sallahli;

import com.pro.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * Handle chat messages between clients and professionals
     */
    @MessageMapping("/chat/{senderType}/{senderId}/{receiverType}/{receiverId}")
    @SendTo("/topic/chat/{receiverType}/{receiverId}")
    public ChatMessage handleChatMessage(
            @DestinationVariable String senderType,
            @DestinationVariable Long senderId,
            @DestinationVariable String receiverType,
            @DestinationVariable Long receiverId,
            @Payload ChatMessage message) {

        log.info("Received chat message from {} {} to {} {}: {}",
                senderType, senderId, receiverType, receiverId, message.getContent());

        // Forward the message through WebSocket service for additional processing
        webSocketService.sendChatMessage(senderId, receiverId, senderType, receiverType, message);

        return message;
    }

    /**
     * Handle professional location updates
     */
    @MessageMapping("/location/pro/{proId}")
    public void handleLocationUpdate(
            @DestinationVariable Long proId,
            @Payload LocationUpdate locationUpdate) {

        log.debug("Received location update from pro {}: lat={}, lng={}",
                proId, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        // Broadcast location update to relevant clients (e.g., those with active jobs)
        webSocketService.sendLocationUpdate(proId, locationUpdate);
    }

    /**
     * Handle professional status updates (online/offline)
     */
    @MessageMapping("/status/pro/{proId}")
    public void handleProStatusUpdate(
            @DestinationVariable Long proId,
            @Payload StatusUpdate statusUpdate) {

        log.info("Pro {} status update: {}", proId, statusUpdate.getStatus());

        // This could trigger lead distribution or other business logic
        // For now, just log the status change
    }

    /**
     * Handle client location sharing for better lead matching
     */
    @MessageMapping("/location/client/{clientId}")
    public void handleClientLocationUpdate(
            @DestinationVariable Long clientId,
            @Payload LocationUpdate locationUpdate) {

        log.debug("Received location update from client {}: lat={}, lng={}",
                clientId, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        // Could be used to update request locations or provide better pro matching
    }

    // DTO classes for WebSocket messages

    public static class ChatMessage {
        private String content;
        private Long timestamp;
        private String messageType; // TEXT, IMAGE, VOICE, etc.

        public ChatMessage() {}

        public ChatMessage(String content, Long timestamp, String messageType) {
            this.content = content;
            this.timestamp = timestamp;
            this.messageType = messageType;
        }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }

    public static class LocationUpdate {
        private Double latitude;
        private Double longitude;
        private Long timestamp;

        public LocationUpdate() {}

        public LocationUpdate(Double latitude, Double longitude, Long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class StatusUpdate {
        private String status; // ONLINE, OFFLINE, BUSY, etc.
        private Long timestamp;

        public StatusUpdate() {}

        public StatusUpdate(String status, Long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
