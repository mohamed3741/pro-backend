package com.pro.dto.sallahli.request;

import com.pro.model.Enum.OsType;
import com.pro.model.Enum.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceRegistrationRequest {

    private String token;
    private OsType osType;
    private String lang;
    private ProfileType profileType;
    private Long clientId; // Only one of clientId or proId should be provided
    private Long proId;
}
