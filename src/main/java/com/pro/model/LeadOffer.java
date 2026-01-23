package com.pro.model;

import com.pro.model.Enum.LeadOfferStatus;
import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lead_offer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadOffer extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lead_offer_id_seq")
    @SequenceGenerator(name = "lead_offer_id_seq", sequenceName = "lead_offer_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomerRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_id", nullable = false)
    private Pro pro;

    @Column(precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false)
    @Builder.Default
    private Long priceMru = 50L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LeadOfferStatus status = LeadOfferStatus.OFFERED;

    private LocalDateTime offeredAt;
    private LocalDateTime expiresAt;

    @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "pro_id"}))
    public static class UniqueRequestProConstraint {}
}
