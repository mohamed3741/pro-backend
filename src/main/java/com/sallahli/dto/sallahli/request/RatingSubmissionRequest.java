package com.sallahli.dto.sallahli.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSubmissionRequest {

    private Long jobId;
    private Integer stars; // 1-5
    private String comment;
}

