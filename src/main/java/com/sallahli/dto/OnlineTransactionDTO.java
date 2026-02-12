package com.sallahli.dto;

import com.sallahli.utils.HasTimestampsDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class OnlineTransactionDTO extends HasTimestampsDTO {

    @Serial
    private static final long serialVersionUID = -6623872071421786983L;

    private Long id;
    private String operationId; // Sedad (idFacture) , Masrivi (purchaseRef)
    private String transactionId;
    private String customerName; // Sedad (prenomPayeur + prenomPayeur) , Masrivi (cname)
    private Integer amount;
    private String paymentRef; // Sedad (numeroRecu)? Masrivi (PaymentRef)
    private String clientPhone;
    private String status;
    private String paymentCode; // Sedad only
    private String errorMessage;
    private String errorCode;
}
