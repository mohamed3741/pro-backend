package com.pro.dto.sallahli.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProOnboardingRequest {

    private String tel;
    private String fullName;
    private String email;
    private Long tradeId; // ServiceCategory ID
    private Long baseZoneId; // Zone ID

    // KYC documents (media IDs)
    private Long cniFrontMediaId;
    private Long cniBackMediaId;
    private Long selfieMediaId;
}
