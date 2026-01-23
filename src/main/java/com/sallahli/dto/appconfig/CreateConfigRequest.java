package com.sallahli.dto.appconfig;

import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConfigRequest {

    @NotNull(message = "App type is required")
    private AppType app;

    @NotNull(message = "Platform type is required")
    private PlatformType platform;

    private String country;

    private String minVersion;
    private String maxVersion;

    @NotNull(message = "Config JSON is required")
    private String configJson;

    private String description;
}

