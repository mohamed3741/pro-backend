package com.sallahli.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.sallahli.model.Notification;
import com.sallahli.model.Pro;
import com.sallahli.model.Enum.OsType;
import com.sallahli.repository.ClientRepository;
import com.sallahli.repository.ProRepository;
import com.sallahli.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserDeviceRepository userDeviceRepository;
    private final ClientRepository clientRepository;
    private final ProRepository proRepository;
    private final ApnsClient apnsClient; // Injected from ApnsConfig

    public void sendToPro(Long proId, Notification notification) {
        // Get pro's devices
        List<com.sallahli.model.UserDevice> devices = userDeviceRepository.findByProId(proId);

        for (com.sallahli.model.UserDevice device : devices) {
            sendPushNotification(device, notification);
        }
    }

    public void sendToClient(Long clientId, Notification notification) {
        // Get client's devices
        List<com.sallahli.model.UserDevice> devices = userDeviceRepository.findByClientId(clientId);

        for (com.sallahli.model.UserDevice device : devices) {
            sendPushNotification(device, notification);
        }
    }

    public void sendToAllPros(Notification notification) {
        // This would be expensive - better to use topics/channels
        List<Pro> pros = proRepository.findAll();
        for (Pro pro : pros) {
            sendToPro(pro.getId(), notification);
        }
    }

    private void sendPushNotification(com.sallahli.model.UserDevice device, Notification notification) {
        if (device.getOsType() == OsType.IOS) {
            sendApnsNotification(device, notification);
        } else if (device.getOsType() == OsType.ANDROID) {
            sendFcmNotification(device, notification);
        } else {
            log.warn("Unsupported OS type for device: {}", device.getOsType());
        }
    }

    private void sendApnsNotification(com.sallahli.model.UserDevice device, Notification notification) {
        try {
            final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
            payloadBuilder.setAlertTitle(notification.getTitle());
            payloadBuilder.setAlertBody(notification.getContent());
            payloadBuilder.setBadge(1);
            payloadBuilder.setSound("default");

            // Add custom data
            payloadBuilder.addCustomProperty("notificationId", notification.getId());
            payloadBuilder.addCustomProperty("type", notification.getType());
            payloadBuilder.addCustomProperty("businessId", notification.getBusinessId());

            final String payload = payloadBuilder.build();
            final String token = device.getToken();

            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, "com.sallahli.app", payload);

            PushNotificationResponse<SimpleApnsPushNotification> response = apnsClient.sendNotification(pushNotification).get();

            if (response.isAccepted()) {
                log.info("APNs notification sent successfully to device: {}", token.substring(0, 10) + "...");
            } else {
                log.error("APNs notification rejected for device {}: {}", token.substring(0, 10) + "...",
                         response.getRejectionReason());
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to send APNs notification to device: {}", device.getToken(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendFcmNotification(com.sallahli.model.UserDevice device, Notification notification) {
        // FCM implementation would go here
        // For now, just log
        log.info("FCM notification would be sent to Android device {}: {}", device.getToken(), notification.getTitle());

        // TODO: Implement FCM integration
        // - Use Firebase Admin SDK
        // - Build FCM message
        // - Send to FCM
    }
}
