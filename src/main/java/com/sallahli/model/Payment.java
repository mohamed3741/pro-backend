package com.sallahli.model;

import com.sallahli.model.Enum.PaymentMethodType;
import com.sallahli.model.Enum.PaymentPurpose;
import com.sallahli.model.Enum.TransactionStatus;
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
public class Payment extends HasTimestamps implements Archivable {

    @Serial
    private static final long serialVersionUID = -2597571074856405435L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer amount;
    private Integer total;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_transaction_id")
    private OnlineTransaction onlineTransaction;

    @Enumerated(EnumType.STRING)
    private PaymentMethodType paymentMethodType;

    @Enumerated(EnumType.STRING)
    private PaymentPurpose paymentPurpose;

    private Integer walletAmount;
    private Integer cashAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_id")
    private Pro pro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_media_id")
    private Media paymentMedia;

    private String paymentRef;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_payment_id")
    private Payment mainPayment;

    @Builder.Default
    private Boolean archived = false;
}
