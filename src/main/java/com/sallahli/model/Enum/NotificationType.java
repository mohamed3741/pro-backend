package com.sallahli.model.Enum;

public enum NotificationType {


    // Sallahli notifications
    NEW_CUSTOMER_REQUEST, // New customer request available for pros
    LEAD_OFFER_RECEIVED, // Pro received a lead offer
    LEAD_ACCEPTED, // Pro accepted the lead
    LEAD_EXPIRED, // Lead offer expired
    LEAD_PURCHASED, // Pro successfully purchased a lead
    WALLET_RECHARGED, // Wallet recharge successful
    WALLET_LOW_BALANCE, // Wallet balance is low
    JOB_STARTED, // Pro started working on job
    JOB_COMPLETED, // Job completed
    JOB_CANCELLED, // Job cancelled
    NEW_RATING, // New rating received
    PRO_ONBOARDED, // Pro successfully onboarded
    PRO_KYC_APPROVED, // Pro KYC approved
    PRO_KYC_REJECTED, // Pro KYC rejected
    PRO_ONLINE_STATUS, // Pro online/offline status change
    CLIENT_REQUEST_UPDATED, // Client request status updated
    CLIENT_REQUEST_CREATED, // Client request created
    PAYMENT_CONFIRMED, // Payment confirmed
    PAYMENT_FAILED,// Payment failed,
    INFORMATION
}
