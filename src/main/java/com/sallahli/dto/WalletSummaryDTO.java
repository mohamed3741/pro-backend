package com.sallahli.dto;

import lombok.*;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class WalletSummaryDTO {

    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

    private Long proId;
    private Long currentBalance;
    private Long totalCredits;
    private Long totalDebits;
    private Long availableBalance;
}
