package com.sallahli.dto;

import com.sallahli.model.Enum.PaymentMethodType;
import com.sallahli.model.Enum.PaymentPurpose;
import com.sallahli.model.Enum.TransactionStatus;
import com.sallahli.utils.HasTimestampsDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class PaymentDTO extends HasTimestampsDTO {

    @Serial
    private static final long serialVersionUID = -2597571074856405435L;

    private Long id;
    private Integer amount;
    private Integer total;
    private TransactionStatus status;
    private OnlineTransactionDTO onlineTransaction;
    private PaymentMethodType paymentMethodType;
    private PaymentPurpose paymentPurpose;
    private Integer walletAmount;
    private Integer cashAmount;
    private Long clientId;
    private Long proId;
    private Long walletId;
    private MediaDTO paymentMedia;
    private String paymentRef;
    private PaymentDTO mainPayment;
}
