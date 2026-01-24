package com.sallahli.model;

import com.sallahli.model.Enum.WalletTransactionType;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pro_wallet_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProWalletTransaction extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pro_wallet_tx_id_seq")
    @SequenceGenerator(name = "pro_wallet_tx_id_seq", sequenceName = "pro_wallet_tx_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_id", nullable = false)
    private Pro pro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;

    @Column(name = "amount", nullable = false)
    private Long amount;

    private String reason; // LEAD_PURCHASE, RECHARGE, FREE_LEADS, etc.

    private String referenceType; // REQUEST, LEAD, PAYMENT, ONLINE_TRANSACTION
    private Long referenceId;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;
}

