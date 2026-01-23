package com.sallahli.dto.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true)
public class MasriviRequestDTO extends PaymentRequestDto {
    private String phoneNumber;
}
