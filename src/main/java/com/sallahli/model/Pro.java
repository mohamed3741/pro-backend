package com.sallahli.model;

import com.sallahli.model.Enum.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pro")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Pro extends User implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pro_id_seq")
    @SequenceGenerator(name = "pro_id_seq", sequenceName = "pro_id_seq", allocationSize = 1)
    private Long id;

    private String fullName;
    private String profilePhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    private Category trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_zone_id")
    private Zone baseZone;

    // KYC document fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cni_front_media_id")
    private Media cniFrontMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cni_back_media_id")
    private Media cniBackMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfie_media_id")
    private Media selfieMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_doc_media_id")
    private Media tradeDocMedia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    private LocalDateTime kycSubmittedAt;
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private Admin approvedByAdmin;

    // Current location (for real-time tracking)
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;

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
    @Column(name = "wallet_balance")
    @Builder.Default
    private Long walletBalance = 0L;

    @Column(name = "low_balance_threshold")
    @Builder.Default
    private Long lowBalanceThreshold = 50L;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isTelVerified = false;

    /*
     * @ManyToMany(fetch = FetchType.LAZY)
     * 
     * @JoinTable(name = "pro_category", joinColumns = @JoinColumn(name = "pro_id"),
     * inverseJoinColumns = @JoinColumn(name = "category_id"))
     * 
     * @Builder.Default
     * private Set<Category> categories = new HashSet<>();
     */

    @Builder.Default
    private Boolean archived = false;
}
