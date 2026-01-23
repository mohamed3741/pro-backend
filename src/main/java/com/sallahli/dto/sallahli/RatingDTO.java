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
public class RatingDTO {

    private Long id;
    private JobDTO job;
    private CustomerRequestDTO request;
    private ClientDTO client;
    private ProDTO pro;
    private Integer stars;
    private String comment;
    private LocalDateTime createdAt;
}
