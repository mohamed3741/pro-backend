package com.sallahli.config;

import com.sallahli.model.Enum.MediaStorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "media-config")
@Data
public class MediaProperties {
    private MediaConfig mediaConfig;

    @Data
    public static class MediaConfig {
        private MediaStorageType type;
        private String token;
        private String url;
        private String folder;
    }

    public MediaConfig getMediaConfig() {
        return mediaConfig;
    }
}

