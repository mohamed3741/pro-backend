package com.pro.model;

import com.pro.model.Enum.KycStatus;
import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pro")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Pro extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pro_id_seq")
    @SequenceGenerator(name = "pro_id_seq", sequenceName = "pro_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tel;

    private String fullName;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private ServiceCategory trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_zone_id")
    private Zone baseZone;

    // KYC fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cni_front_media_id")
    private Media cniFrontMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cni_back_media_id")
    private Media cniBackMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfie_media_id")
    private Media selfieMedia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    private LocalDateTime approvedAt;

    private Long approvedBy;

    // Operational fields
    @Builder.Default
    private Boolean online = false;

    @Builder.Default
    private Double ratingAvg = 5.00;

    @Builder.Default
    private Long ratingCount = 0L;

    @Builder.Default
    private Long jobsCompleted = 0L;

    // Wallet fields (prepaid)
    @Builder.Default
    private Long walletBalanceMru = 0L;

    @Builder.Default
    private Long lowBalanceThresholdMru = 50L;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isDeleted = false;
}
