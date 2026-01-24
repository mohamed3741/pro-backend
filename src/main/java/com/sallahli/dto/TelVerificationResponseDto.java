package com.sallahli.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelVerificationResponseDto {
    private Boolean isExist;
    private Boolean isVerified;
}

