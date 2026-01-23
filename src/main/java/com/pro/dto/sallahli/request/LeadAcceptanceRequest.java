package com.pro.dto.sallahli.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAcceptanceRequest {

    private Long leadOfferId;
    private Long proId;
}
