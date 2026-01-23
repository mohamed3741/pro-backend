package com.sallahli.dto.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true)
public class BankilyRequestDTO extends PaymentRequestDto {
    private String passCode;
    private String clientPhone;
    private String firstName;
    private String lastName;
}
