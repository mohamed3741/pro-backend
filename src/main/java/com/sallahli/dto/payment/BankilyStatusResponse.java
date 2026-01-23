package com.sallahli.dto.payment;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class BankilyStatusResponse {
    private String status;
    private String transactionId;
    private String errorCode;
    private String errorMessage;
}
