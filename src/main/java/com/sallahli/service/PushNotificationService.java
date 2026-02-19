package com.sallahli.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.google.firebase.messaging.*;
import com.sallahli.config.ApnsConfig;
import com.sallahli.model.Enum.OsType;
import com.sallahli.model.Enum.ProfileType;
import com.sallahli.model.Notification;
import com.sallahli.model.UserDevice;
import com.sallahli.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PushNotificationService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserDeviceService userDeviceService;
    private final FirebaseMessaging firebaseMessaging;
    private final ApnsClient clientApnsClient;
    private final ApnsClient proApnsClient;
    private final ApnsConfig apnsConfig;

    public PushNotificationService(
            UserDeviceRepository userDeviceRepository,
            UserDeviceService userDeviceService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) FirebaseMessaging firebaseMessaging,
            @Qualifier("clientApnsClient") @org.springframework.beans.factory.annotation.Autowired(required = false) ApnsClient clientApnsClient,
            @Qualifier("proApnsClient") @org.springframework.beans.factory.annotation.Autowired(required = false) ApnsClient proApnsClient,
            ApnsConfig apnsConfig) {
        this.userDeviceRepository = userDeviceRepository;
        this.userDeviceService = userDeviceService;
        this.firebaseMessaging = firebaseMessaging;
        this.clientApnsClient = clientApnsClient;
        this.proApnsClient = proApnsClient;
        this.apnsConfig = apnsConfig;
    }

    // ========================================================================
    // High-Level Send Methods
    // ========================================================================

    public void sendToPro(Long proId, Notification notification) {
        List<UserDevice> devices = userDeviceRepository.findByProId(proId);

        if (devices.isEmpty()) {
            log.warn("No devices registered for pro: {}", proId);
            return;
        }

        for (UserDevice device : devices) {
            sendToDevice(device, notification, ProfileType.PRO);
        }
    }

    public void sendToClient(Long clientId, Notification notification) {
        List<UserDevice> devices = userDeviceRepository.findByClientId(clientId);

        if (devices.isEmpty()) {
            log.warn("No devices registered for client: {}", clientId);
            return;
        }

        for (UserDevice device : devices) {
            sendToDevice(device, notification, ProfileType.CLIENT);
        }
    }

    public void sendToAllByProfileType(ProfileType profileType, Notification notification) {
        List<UserDevice> devices = userDeviceRepository.findByProfileType(profileType);
        log.info("Sending notification to {} {} devices", devices.size(), profileType);

        for (UserDevice device : devices) {
            sendToDevice(device, notification, profileType);
        }
    }

    public void sendToToken(String token, Notification notification, ProfileType profileType) {
        userDeviceRepository.findByToken(token).ifPresent(device -> sendToDevice(device, notification, profileType));
    }

    // ========================================================================
    // Platform-Specific Send Methods
    // ========================================================================

    private void sendToDevice(UserDevice device, Notification notification, ProfileType profileType) {
        try {
            switch (device.getOsType()) {
                case IOS:
                    sendApnsNotification(device, notification, profileType);
                    break;
                case ANDROID:
                case EXPO:
                    sendFcmNotification(device, notification);
                    break;
                case WEB:
                    sendWebPushNotification(device, notification);
                    break;
                default:
                    log.warn("Unsupported OS type for device: {}", device.getOsType());
            }
        } catch (Exception e) {
            log.error("Failed to send push notification to device {}: {}",
                    truncateToken(device.getToken()), e.getMessage());
        }
    }

    // ========================================================================
    // iOS (APNs) Notifications
    // ========================================================================

    private void sendApnsNotification(UserDevice device, Notification notification, ProfileType profileType) {
        ApnsClient apnsClient = profileType == ProfileType.PRO ? proApnsClient : clientApnsClient;
        String bundleId = profileType == ProfileType.PRO
                ? apnsConfig.getProBundle()
                : apnsConfig.getClientBundle();

        if (apnsClient == null) {
            log.warn("APNs client not configured for profile type: {}", profileType);
            return;
        }

        try {
            // Manually build JSON payload to avoid ApnsPayloadBuilder compatibility issues
            Map<String, Object> aps = new HashMap<>();
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", notification.getTitle());
            alert.put("body", notification.getContent());

            aps.put("alert", alert);
            aps.put("badge", 1);
            aps.put("sound", "default");

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("aps", aps);

            // Add custom data
            if (notification.getId() != null) {
                payloadMap.put("notificationId", notification.getId());
            }
            if (notification.getType() != null) {
                payloadMap.put("type", notification.getType().name());
            }
            if (notification.getBusinessId() != null) {
                payloadMap.put("businessId", notification.getBusinessId());
            }
            if (notification.getServedApp() != null) {
                payloadMap.put("servedApp", notification.getServedApp());
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final String payload = mapper.writeValueAsString(payloadMap);
            final String token = device.getToken();

            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                    token, bundleId, payload);

            PushNotificationResponse<SimpleApnsPushNotification> response = apnsClient
                    .sendNotification(pushNotification).get();

            if (response.isAccepted()) {
                log.debug("APNs notification sent successfully to device: {}...", truncateToken(token));
            } else {
                log.error("APNs notification rejected for device {}...: {}",
                        truncateToken(token), response.getRejectionReason());

                // Handle invalid tokens
                if ("BadDeviceToken".equals(response.getRejectionReason()) ||
                        "Unregistered".equals(response.getRejectionReason())) {
                    userDeviceService.removeInvalidToken(token);
                }
            }

        } catch (InterruptedException e) {
            log.error("APNs send interrupted for device: {}", truncateToken(device.getToken()));
            Thread.currentThread().interrupt();
        } catch (ExecutionException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("APNs send failed for device {}: {}", truncateToken(device.getToken()), e.getMessage());
        }
    }

    // ========================================================================
    // Android/Expo (FCM) Notifications
    // ========================================================================

    private void sendFcmNotification(UserDevice device, Notification notification) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not configured. Cannot send FCM notification.");
            return;
        }

        try {
            // Build FCM message with both notification and data payload
            Message.Builder messageBuilder = Message.builder()
                    .setToken(device.getToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getContent())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                    .build())
                            .build());

            // Add custom data payload
            Map<String, String> data = new HashMap<>();
            if (notification.getId() != null) {
                data.put("notificationId", notification.getId().toString());
            }
            if (notification.getType() != null) {
                data.put("type", notification.getType().name());
            }
            if (notification.getBusinessId() != null) {
                data.put("businessId", notification.getBusinessId().toString());
            }
            if (notification.getServedApp() != null) {
                data.put("servedApp", notification.getServedApp());
            }

            if (!data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            log.debug("FCM notification sent successfully. Message ID: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for device {}: {}", truncateToken(device.getToken()), e.getMessage());

            // Handle invalid tokens
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                userDeviceService.removeInvalidToken(device.getToken());
            }
        }
    }

    // ========================================================================
    // Web Push Notifications
    // ========================================================================

    private void sendWebPushNotification(UserDevice device, Notification notification) {
        // For web push, we use FCM with web configuration
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not configured for web push.");
            return;
        }

        try {
            // Build web push specific message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(device.getToken())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(notification.getTitle())
                                    .setBody(notification.getContent())
                                    .setIcon("/icons/notification-icon.png")
                                    .setBadge("/icons/badge-icon.png")
                                    .build())
                            .setFcmOptions(WebpushFcmOptions.builder()
                                    .setLink("https://app.sallahli.com")
                                    .build())
                            .build());

            // Add data payload
            Map<String, String> data = new HashMap<>();
            if (notification.getId() != null) {
                data.put("notificationId", notification.getId().toString());
            }
            if (notification.getType() != null) {
                data.put("type", notification.getType().name());
            }
            if (notification.getBusinessId() != null) {
                data.put("businessId", notification.getBusinessId().toString());
            }

            if (!data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            log.debug("Web push notification sent successfully. Message ID: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Web push send failed for device {}: {}", truncateToken(device.getToken()), e.getMessage());

            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                userDeviceService.removeInvalidToken(device.getToken());
            }
        }
    }

    // ========================================================================
    // Topic-Based Notifications (for broadcasts)
    // ========================================================================

    public void sendToTopic(String topic, Notification notification) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not configured. Cannot send topic notification.");
            return;
        }

        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getContent())
                            .build())
                    .putData("type", notification.getType() != null ? notification.getType().name() : "GENERAL")
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Topic notification sent to '{}'. Message ID: {}", topic, response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send topic notification to '{}': {}", topic, e.getMessage());
        }
    }

    public void subscribeToTopic(String token, String topic) {
        if (firebaseMessaging == null) {
            return;
        }

        try {
            firebaseMessaging.subscribeToTopic(List.of(token), topic);
            log.debug("Device subscribed to topic: {}", topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe device to topic '{}': {}", topic, e.getMessage());
        }
    }

    public void unsubscribeFromTopic(String token, String topic) {
        if (firebaseMessaging == null) {
            return;
        }

        try {
            firebaseMessaging.unsubscribeFromTopic(List.of(token), topic);
            log.debug("Device unsubscribed from topic: {}", topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to unsubscribe device from topic '{}': {}", topic, e.getMessage());
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private String truncateToken(String token) {
        if (token == null || token.length() < 10) {
            return token;
        }
        return token.substring(0, 10) + "...";
    }
}
