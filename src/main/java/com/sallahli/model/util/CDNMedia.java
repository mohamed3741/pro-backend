package com.sallahli.model.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDNMedia {
    private CDNResult result;
    private Boolean success;
    private List<String> errors;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CDNResult {
        private String id;
        private String filename;
        private String uploaded;  // Cloudflare returns this as an ISO-8601 timestamp string
        private Boolean requireSignedURLs;
        private List<String> variants;
    }
}

