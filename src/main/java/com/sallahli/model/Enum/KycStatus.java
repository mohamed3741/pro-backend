package com.sallahli.model.Enum;

public enum KycStatus {
    NOT_STARTED, // Driver has not yet submitted documents
    PENDING, // Waiting for admin approval
    APPROVED, // KYC approved, pro can work
    REJECTED // KYC rejected, pro cannot work
}
