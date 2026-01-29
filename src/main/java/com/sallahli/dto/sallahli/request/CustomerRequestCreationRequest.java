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


    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressText;
    private String landmark;


    private String descriptionText;
    private Long voiceNoteMediaId;
    private Boolean urgent;


    private List<Long> attachedMediaIds;
}

