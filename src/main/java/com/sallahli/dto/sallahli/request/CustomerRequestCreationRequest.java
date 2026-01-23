package com.sallahli.dto.sallahli.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestCreationRequest {

    private Long clientId;
    private Long categoryId;

    // Location
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressText;
    private String landmark;

    // Request details
    private String descriptionText;
    private Long voiceNoteMediaId;
    private Boolean urgent;

    // Attached media
    private List<Long> attachedMediaIds;
}

