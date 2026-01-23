package com.sallahli.model.Enum;

public enum TransactionStatus {
    INCOMPLETE,     // Payment initiated but not completed
    IN_PROGRESS,    // Payment is being processed
    SUCCEEDED,      // Payment completed successfully
    FAILED,         // Payment failed
    CANCELLED       // Payment was cancelled
}
