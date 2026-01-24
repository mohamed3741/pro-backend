package com.sallahli.dto;

import com.sallahli.model.Enum.DeliveryMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCode {
    public String username;
    public String code;
    public DeliveryMethod method;
}


