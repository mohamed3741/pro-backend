package com.sallahli.dto.sallahli;

import com.sallahli.dto.MediaDTO;
import com.sallahli.model.Enum.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {

    private Long id;
    private ClientDTO client;
    private CategoryDTO category;


    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressText;
    private String landmark;


    private String descriptionText;
    private MediaDTO voiceNoteMedia;
    private RequestStatus status;
    private Boolean urgent;


    private LocalDateTime broadcastedAt;
    private LocalDateTime expiresAt;


    private List<MediaDTO> attachedMedia;
    private Integer nearbyProsCount;
    private LocalDateTime createdAt;
}

