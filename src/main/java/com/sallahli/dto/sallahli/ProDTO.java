package com.sallahli.dto.sallahli;

import com.sallahli.model.Enum.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProDTO {

    private Long id;
    private String tel;
    private String fullName;
    private String email;
    private ServiceCategoryDTO trade;
    private ZoneDTO baseZone;

    // KYC fields
    private MediaDTO cniFrontMedia;
    private MediaDTO cniBackMedia;
    private MediaDTO selfieMedia;
    private KycStatus kycStatus;
    private LocalDateTime approvedAt;
    private Long approvedBy;

    // Operational fields
    private Boolean online;
    private Double ratingAvg;
    private Long ratingCount;
    private Long jobsCompleted;

    // Wallet fields
    private Long walletBalanceMru;
    private Long lowBalanceThresholdMru;

    private Boolean isActive;
    private Boolean isDeleted;
}

