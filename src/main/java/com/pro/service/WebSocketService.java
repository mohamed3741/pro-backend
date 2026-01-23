package com.pro.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Topics for different types of real-time updates
    private static final String TOPIC_LEADS = "/topic/leads";
    private static final String TOPIC_REQUESTS = "/topic/requests";
    private static final String TOPIC_JOBS = "/topic/jobs";
    private static final String TOPIC_NOTIFICATIONS = "/topic/notifications";

    /**
     * Send lead offers to nearby professionals
     */
    public void broadcastLeadOffers(String tradeCode, Object leadOfferData) {
        String topic = TOPIC_LEADS + "/" + tradeCode;
        messagingTemplate.convertAndSend(topic, leadOfferData);
        log.debug("Broadcasted lead offer to trade: {}", tradeCode);
    }

    /**
     * Send request updates to clients
     */
    public void sendRequestUpdateToClient(Long clientId, Object requestUpdate) {
        String topic = TOPIC_REQUESTS + "/client/" + clientId;
        messagingTemplate.convertAndSend(topic, requestUpdate);
        log.debug("Sent request update to client: {}", clientId);
    }

    /**
     * Send job updates to professionals
     */
    public void sendJobUpdateToPro(Long proId, Object jobUpdate) {
        String topic = TOPIC_JOBS + "/pro/" + proId;
        messagingTemplate.convertAndSend(topic, jobUpdate);
        log.debug("Sent job update to pro: {}", proId);
    }

    /**
     * Send notifications to users
     */
    public void sendNotificationToUser(Long userId, String userType, Object notification) {
        String topic = TOPIC_NOTIFICATIONS + "/" + userType.toLowerCase() + "/" + userId;
        messagingTemplate.convertAndSend(topic, notification);
        log.debug("Sent notification to {}: {}", userType, userId);
    }

    /**
     * Send wallet updates to professionals
     */
    public void sendWalletUpdateToPro(Long proId, Object walletUpdate) {
        String topic = "/topic/wallet/pro/" + proId;
        messagingTemplate.convertAndSend(topic, walletUpdate);
        log.debug("Sent wallet update to pro: {}", proId);
    }

    /**
     * Send general announcements to all users
     */
    public void broadcastAnnouncement(Object announcement) {
        messagingTemplate.convertAndSend("/topic/announcements", announcement);
        log.debug("Broadcasted announcement to all users");
    }

    /**
     * Send real-time location updates (for tracking professionals)
     */
    public void sendLocationUpdate(Long proId, Object locationData) {
        String topic = "/topic/location/pro/" + proId;
        messagingTemplate.convertAndSend(topic, locationData);
        log.debug("Sent location update for pro: {}", proId);
    }

    /**
     * Send chat messages between clients and professionals
     */
    public void sendChatMessage(Long senderId, Long receiverId, String senderType, String receiverType, Object message) {
        // Send to receiver
        String receiverTopic = "/topic/chat/" + receiverType.toLowerCase() + "/" + receiverId;
        messagingTemplate.convertAndSend(receiverTopic, message);

        // Also send back to sender for confirmation
        String senderTopic = "/topic/chat/" + senderType.toLowerCase() + "/" + senderId;
        messagingTemplate.convertAndSend(senderTopic, message);

        log.debug("Sent chat message from {} {} to {} {}", senderType, senderId, receiverType, receiverId);
    }
}
