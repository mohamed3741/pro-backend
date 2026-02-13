package com.sallahli.dto.sallahli;

import com.sallahli.dto.MediaDTO;
import com.sallahli.dto.UserDTO;
import com.sallahli.model.Enum.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProDTO extends UserDTO {

    private String fullName;
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
    private AdminDTO approvedByAdmin;

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
    private Boolean isTelVerified;
    private Boolean archived;
}
