package com.sallahli.dto.sallahli.request;

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
    private Long tradeId;
    private Long baseZoneId;

    private Long cniFrontMediaId;
    private Long cniBackMediaId;
    private Long selfieMediaId;
}
