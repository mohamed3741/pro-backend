package com.sallahli.model.Enum;

public enum RequestStatus {
    BROADCASTED, // Sent to nearby pros
    ASSIGNED, // Lead accepted by a pro
    CANCELLED, // Request cancelled by client
    EXPIRED, // Request expired without acceptance
    DONE // Job completed
}
