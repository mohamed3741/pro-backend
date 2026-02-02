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
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String profilePhoto;
    private CategoryDTO trade;
    private ZoneDTO baseZone;

    private MediaDTO cniFrontMedia;
    private MediaDTO cniBackMedia;
    private MediaDTO selfieMedia;
    private MediaDTO tradeDocMedia;
    private KycStatus kycStatus;
    private LocalDateTime kycSubmittedAt;
    private LocalDateTime approvedAt;
    private Long approvedBy;

    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;

    private Boolean online;
    private Double ratingAvg;
    private Long ratingCount;
    private Long jobsCompleted;

    private Long walletBalance;
    private Long lowBalanceThreshold;

    private Boolean isActive;
    private Boolean archived;
}
