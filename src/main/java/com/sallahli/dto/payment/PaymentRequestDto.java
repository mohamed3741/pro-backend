package com.sallahli.dto.payment;

import com.sallahli.model.Enum.PaymentMethodType;
import com.sallahli.model.Enum.PaymentPurpose;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class PaymentRequestDto {
    private PaymentMethodType paymentMethodType;
    private PaymentPurpose paymentPurpose;
    private Integer amount;
    private boolean useWallet;
    private String phoneNumber; // For mobile payment methods
}
