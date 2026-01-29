package com.sallahli.dto.sallahli;

import com.sallahli.dto.MediaDTO;
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
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String profilePhoto;
    private CategoryDTO trade;
    private ZoneDTO baseZone;

    // KYC document fields
    private MediaDTO cniFrontMedia;
    private MediaDTO cniBackMedia;
    private MediaDTO selfieMedia;
    private MediaDTO tradeDocMedia;
    private KycStatus kycStatus;
    private LocalDateTime kycSubmittedAt;
    private LocalDateTime approvedAt;
    private Long approvedBy;

    // Current location
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;

    // Operational fields
    private Boolean online;
    private Double ratingAvg;
    private Long ratingCount;
    private Long jobsCompleted;

    // Wallet fields
    private Long walletBalance;
    private Long lowBalanceThreshold;

    private Boolean isActive;
    private Boolean archived;
}
