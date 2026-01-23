package com.sallahli.controller;

import com.sallahli.dto.appconfig.*;
import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import com.sallahli.service.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "App Configuration", description = "APIs for managing application configurations for different platforms")
public class AppConfigController {

    private final AppConfigService configService;


    @GetMapping("/get")
    @Operation(summary = "Get configuration for app platform",
               description = "Retrieves configuration based on app type, platform, version, and country")
    public ResponseEntity<?> getConfig(
        @RequestParam AppType app,
        @RequestParam PlatformType platform,
        @RequestParam String version,
        @RequestParam(required = false) String country,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {

        if (ifNoneMatch != null && configService.isConfigValid(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .eTag(ifNoneMatch)
                .build();
        }

        ConfigQueryRequest request = ConfigQueryRequest.builder()
            .app(app)
            .platform(platform)
            .version(version)
            .country(country)
            .build();

        return configService.getConfig(request)
            .map(response -> ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(response.getCacheMaxAge(), TimeUnit.SECONDS).cachePublic())
                .eTag(response.getEtag())
                .body(response.getConfig()))
            .orElseGet(() -> {
                log.warn("No matching config found for request: {}", request);
                return ResponseEntity.notFound().build();
            });
    }


    @PostMapping("/create")
    @Operation(summary = "Create new configuration")
    public ResponseEntity<AppConfigBundleDto> createConfig(
        @Valid @RequestBody CreateConfigRequest request
    ) {
        // In a real application, you'd get the user from security context
        String createdBy = "system"; // authentication.getName()
        AppConfigBundleDto created = configService.createConfig(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping("/{configId}")
    @Operation(summary = "Update existing configuration")
    public ResponseEntity<AppConfigBundleDto> updateConfig(
        @PathVariable Long configId,
        @Valid @RequestBody CreateConfigRequest request
    ) {
        // In a real application, you'd get the user from security context
        String createdBy = "system"; // authentication.getName()
        AppConfigBundleDto updated = configService.updateConfig(configId, request, createdBy);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete configuration")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get configuration by ID")
    public ResponseEntity<AppConfigBundleDto> getConfigById(@PathVariable Long id) {
        AppConfigBundleDto config = configService.getConfigById(id);
        return ResponseEntity.ok(config);
    }


    @GetMapping("/list")
    @Operation(summary = "List configurations")
    public ResponseEntity<List<AppConfigBundleDto>> listConfigs(
        @RequestParam(required = false) AppType app,
        @RequestParam(required = false) PlatformType platform
    ) {
        List<AppConfigBundleDto> configs = configService.listConfigs(app, platform);
        return ResponseEntity.ok(configs);
    }


    @GetMapping("/active")
    @Operation(summary = "List all active configurations")
    public ResponseEntity<List<AppConfigBundleDto>> listActiveConfigs() {
        List<AppConfigBundleDto> configs = configService.listAllActiveConfigs();
        return ResponseEntity.ok(configs);
    }
}

