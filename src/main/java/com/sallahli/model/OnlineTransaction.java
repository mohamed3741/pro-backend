package com.sallahli.model;

import com.sallahli.model.Enum.BankType;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class OnlineTransaction extends HasTimestamps {

    @Serial
    private static final long serialVersionUID = -6623872071421786983L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Enumerated(EnumType.STRING)
    private BankType bankType;
}
