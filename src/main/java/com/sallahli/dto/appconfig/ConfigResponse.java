package com.sallahli.dto.appconfig;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigResponse {

    private JsonNode config;
    private String etag;
    private Long cacheMaxAge;
}

