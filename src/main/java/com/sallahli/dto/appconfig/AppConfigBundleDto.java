package com.sallahli.dto.appconfig;

import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import com.sallahli.utils.HasTimestampsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigBundleDto extends HasTimestampsDTO {

    private Long id;
    private AppType app;
    private PlatformType platform;
    private String country;
    private String minVersion;
    private String maxVersion;
    private boolean isActive;
    private String configJson;
    private String createdBy;
    private String hash;
    private String description;
}

