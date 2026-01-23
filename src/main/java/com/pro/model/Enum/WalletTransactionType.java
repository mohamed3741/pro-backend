package com.pro.model.Enum;

public enum WalletTransactionType {
    CREDIT,     // Money added to wallet (recharge)
    DEBIT,      // Money deducted from wallet (lead purchase)
    REFUND,     // Money refunded
    ADJUSTMENT  // Manual adjustment by admin
}
