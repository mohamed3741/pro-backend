package com.sallahli.service;

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

    
    public void broadcastLeadOffers(String tradeCode, Object leadOfferData) {
        String topic = TOPIC_LEADS + "/" + tradeCode;
        messagingTemplate.convertAndSend(topic, leadOfferData);
        log.debug("Broadcasted lead offer to trade: {}", tradeCode);
    }

    
    public void sendRequestUpdateToClient(Long clientId, Object requestUpdate) {
        String topic = TOPIC_REQUESTS + "/client/" + clientId;
        messagingTemplate.convertAndSend(topic, requestUpdate);
        log.debug("Sent request update to client: {}", clientId);
    }

    
    public void sendJobUpdateToPro(Long proId, Object jobUpdate) {
        String topic = TOPIC_JOBS + "/pro/" + proId;
        messagingTemplate.convertAndSend(topic, jobUpdate);
        log.debug("Sent job update to pro: {}", proId);
    }

    
    public void sendNotificationToUser(Long userId, String userType, Object notification) {
        String topic = TOPIC_NOTIFICATIONS + "/" + userType.toLowerCase() + "/" + userId;
        messagingTemplate.convertAndSend(topic, notification);
        log.debug("Sent notification to {}: {}", userType, userId);
    }

    
    public void sendWalletUpdateToPro(Long proId, Object walletUpdate) {
        String topic = "/topic/wallet/pro/" + proId;
        messagingTemplate.convertAndSend(topic, walletUpdate);
        log.debug("Sent wallet update to pro: {}", proId);
    }

    
    public void broadcastAnnouncement(Object announcement) {
        messagingTemplate.convertAndSend("/topic/announcements", announcement);
        log.debug("Broadcasted announcement to all users");
    }

    
    public void sendLocationUpdate(Long proId, Object locationData) {
        String topic = "/topic/location/pro/" + proId;
        messagingTemplate.convertAndSend(topic, locationData);
        log.debug("Sent location update for pro: {}", proId);
    }

    
    public void sendChatMessage(Long senderId, Long receiverId, String senderType, String receiverType, Object message) {
        // Send to receiver
        String receiverTopic = "/topic/chat/" + receiverType.toLowerCase() + "/" + receiverId;
        messagingTemplate.convertAndSend(receiverTopic, message);

        // Also send back to sender for confirmation
        String senderTopic = "/topic/chat/" + senderType.toLowerCase() + "/" + senderId;
        messagingTemplate.convertAndSend(senderTopic, message);

        log.debug("Sent chat message from {} {} to {} {}", senderType, senderId, receiverType, receiverId);
    }

    
    public void sendChatMessageToConversation(Long conversationId, Object message) {
        String conversationTopic = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(conversationTopic, message);
        log.debug("Sent chat message to conversation {}", conversationId);
    }

    
    public void sendChatNotification(Long userId, String userType, Object notification) {
        String topic = "/topic/notifications/chat/" + userType.toLowerCase() + "/" + userId;
        messagingTemplate.convertAndSend(topic, notification);
        log.debug("Sent chat notification to {}: {}", userType, userId);
    }

    
    public void sendConversationUpdate(Long conversationId, Object update) {
        String topic = "/topic/conversation/" + conversationId + "/updates";
        messagingTemplate.convertAndSend(topic, update);
        log.debug("Sent conversation update to conversation {}", conversationId);
    }
}

