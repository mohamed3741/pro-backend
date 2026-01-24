package com.sallahli.service;

import com.sallahli.model.*;
import com.sallahli.model.Enum.NotificationType;
import com.sallahli.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;



    @Transactional
    public void sendLeadAcceptedNotification(Pro pro, LeadAcceptance acceptance) {
        Notification notification = createNotification(
                NotificationType.LEAD_ACCEPTED,
                "Lead Accepted",
                "You have successfully accepted a lead",
                acceptance.getId(),
                "LEAD_ACCEPTANCE"
        );

        pushNotificationService.sendToPro(pro.getId(), notification);
    }

    @Transactional
    public void sendRequestAssignedNotification(Client client, LeadAcceptance acceptance) {
        Notification notification = createNotification(
                NotificationType.CLIENT_REQUEST_UPDATED,
                "Request Assigned",
                "A professional has been assigned to your request",
                acceptance.getRequest().getId(),
                "CUSTOMER_REQUEST"
        );

        pushNotificationService.sendToClient(client.getId(), notification);
    }

    @Transactional
    public void sendLeadExpiredNotification(Pro pro, LeadOffer offer) {
        Notification notification = createNotification(
                NotificationType.LEAD_EXPIRED,
                "Lead Expired",
                "The lead offer has expired",
                offer.getId(),
                "LEAD_OFFER"
        );

        pushNotificationService.sendToPro(pro.getId(), notification);
    }

    @Transactional
    public void sendWalletRechargeNotification(Pro pro, Long amount) {
        Notification notification = createNotification(
                NotificationType.WALLET_RECHARGED,
                "Wallet Recharged",
                String.format("Your wallet has been recharged with %d MRU", amount),
                pro.getId(),
                "PRO"
        );

        pushNotificationService.sendToPro(pro.getId(), notification);
    }

    @Transactional
    public void sendLowBalanceAlert(Pro pro) {
        Notification notification = createNotification(
                NotificationType.WALLET_LOW_BALANCE,
                "Low Balance Alert",
                "Your wallet balance is low. Please recharge to continue receiving leads",
                pro.getId(),
                "PRO"
        );

        pushNotificationService.sendToPro(pro.getId(), notification);
    }

    @Transactional
    public void sendJobStartedNotification(Job job) {
        Notification notification = createNotification(
                NotificationType.JOB_STARTED,
                "Job Started",
                "The professional has started working on your request",
                job.getId(),
                "JOB"
        );

        pushNotificationService.sendToClient(job.getClient().getId(), notification);
    }

    @Transactional
    public void sendJobCompletedNotification(Job job) {
        Notification notification = createNotification(
                NotificationType.JOB_COMPLETED,
                "Job Completed",
                "Your job has been completed. Please rate the service",
                job.getId(),
                "JOB"
        );

        pushNotificationService.sendToClient(job.getClient().getId(), notification);
    }

    @Transactional
    public void sendJobCancelledNotification(Job job, String reason) {
        Notification notification = createNotification(
                NotificationType.JOB_CANCELLED,
                "Job Cancelled",
                "Your job has been cancelled. Reason: " + reason,
                job.getId(),
                "JOB"
        );

        pushNotificationService.sendToClient(job.getClient().getId(), notification);
    }

    private Notification createNotification(NotificationType type, String title, String content,
                                          Long businessId, String servedApp) {
        return Notification.builder()
                .type(type)
                .title(title)
                .content(content)
                .businessId(businessId)
                .servedApp(servedApp)
                .build();
    }
}
