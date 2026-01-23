package com.pro.dto.sallahli;

import com.pro.model.Enum.LeadOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadOfferDTO {

    private Long id;
    private CustomerRequestDTO request;
    private ProDTO pro;
    private BigDecimal distanceKm;
    private Long priceMru;
    private LeadOfferStatus status;
    private LocalDateTime offeredAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
