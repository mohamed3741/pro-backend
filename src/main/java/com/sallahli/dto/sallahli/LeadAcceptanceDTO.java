package com.sallahli.dto.sallahli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAcceptanceDTO {

    private Long id;
    private LeadOfferDTO leadOffer;
    private CustomerRequestDTO request;
    private ProDTO pro;
    private Long priceMru;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}

