package com.pro.model.Enum;

public enum NotificationType {

    // Medical clinic notifications
    CHANGE_OF_OPENING_STATUS ,
    CHANGE_SUPPLEMENT_AVAILABILITY ,
    CHANGE_PRODUCT_AVAILABILITY,

    ORDER_CREATED,
    ORDER_ACCEPTED,
    ORDER_CANCELED,
    ORDER_READY_TO_PICKUP,
    ORDER_PICKED_UP,
    ORDER_UPDATED_BY_MANAGER,
    ORDER_UPDATED_BY_CLIENT,
    ORDER_DELIVERED,
    ORDER_REJECTED,
    ORDER_PROPOSED,
    SCHEDULED_ORDER_TIME,
    INFORMATION,
    NEW_BANNER,
    BANNER_APPROVED,
    BANNER_DISAPPROVED,
    NEW_ORDER,
    PAYMENT_SUCCESS,
    PAYMENT_FAILURE,
    NEW_CALL,
    HEARTBEAT,
    DRIVER_LOCATION,
    ORDER_AFFECTED,
    ORDER_DISAFFECTED,
    CHAT,

    PRODUCT_APPROVED,
    PRODUCT_REJECTED,
    PRODUCT_PUBLISHED,
    PRODUCT_DISAPPROVED,

    // Sallahli notifications
    NEW_CUSTOMER_REQUEST,          // New customer request available for pros
    LEAD_OFFER_RECEIVED,           // Pro received a lead offer
    LEAD_ACCEPTED,                 // Pro accepted the lead
    LEAD_EXPIRED,                  // Lead offer expired
    LEAD_PURCHASED,                // Pro successfully purchased a lead
    WALLET_RECHARGED,              // Wallet recharge successful
    WALLET_LOW_BALANCE,            // Wallet balance is low
    JOB_STARTED,                   // Pro started working on job
    JOB_COMPLETED,                 // Job completed
    JOB_CANCELLED,                 // Job cancelled
    NEW_RATING,                    // New rating received
    PRO_ONBOARDED,                 // Pro successfully onboarded
    PRO_KYC_APPROVED,              // Pro KYC approved
    PRO_KYC_REJECTED,              // Pro KYC rejected
    PRO_ONLINE_STATUS,             // Pro online/offline status change
    CLIENT_REQUEST_UPDATED,        // Client request status updated
    PAYMENT_CONFIRMED,             // Payment confirmed
    PAYMENT_FAILED                 // Payment failed
}


