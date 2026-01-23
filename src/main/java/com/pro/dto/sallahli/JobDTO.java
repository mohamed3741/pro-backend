package com.pro.dto.sallahli;

import com.pro.model.Enum.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {

    private Long id;
    private CustomerRequestDTO request;
    private LeadAcceptanceDTO acceptance;
    private ProDTO pro;
    private ClientDTO client;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime doneAt;
    private LocalDateTime createdAt;
}
