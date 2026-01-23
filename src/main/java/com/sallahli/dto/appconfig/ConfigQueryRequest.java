package com.sallahli.dto.appconfig;

import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigQueryRequest {

    private AppType app;
    private PlatformType platform;
    private String version;
    private String country;
}

