package com.sallahli.model.Enum;

public enum LeadOfferStatus {
    OFFERED, // Lead offered to pro
    PENDING_CLIENT_APPROVAL, // Pro proposed a price, waiting for client
    ACCEPTED, // Pro accepted the lead (or client accepted the proposed price)
    MISSED, // Pro didn't respond in time
    EXPIRED, // Offer expired
    CANCELLED // Offer cancelled
}
