package com.sallahli.dto;

import com.sallahli.security.constant.LoginProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ExchangeableTokenDto {
    String externalToken;
    LoginProvider loginProvider;
}
