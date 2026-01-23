package com.sallahli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallahli.dto.appconfig.*;
import com.sallahli.mapper.AppConfigBundleMapper;
import com.sallahli.model.AppConfigBundle;
import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import com.sallahli.repository.AppConfigBundleRepository;
import com.sallahli.utils.VersionComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppConfigService {

    private final AppConfigBundleRepository repository;
    private final AppConfigBundleMapper mapper;
    private final ObjectMapper objectMapper;

    private static final long DEFAULT_CACHE_MAX_AGE = 3600L; // 1 hour


    @Transactional(readOnly = true)
    public Optional<ConfigResponse> getConfig(ConfigQueryRequest request) {

        List<AppConfigBundle> candidates = repository.findMatchingConfigs(
            request.getApp(),
            request.getPlatform(),
            request.getCountry()
        );

        Optional<AppConfigBundle> matchedConfig = candidates.stream()
            .filter(config -> VersionComparator.isInRange(
                request.getVersion(),
                config.getMinVersion(),
                config.getMaxVersion()
            ))
            .findFirst();

        return matchedConfig.map(config -> {
            try {
                JsonNode configJson = objectMapper.readTree(config.getConfigJson());
                return ConfigResponse.builder()
                    .config(configJson)
                    .etag(config.getHash())
                    .cacheMaxAge(DEFAULT_CACHE_MAX_AGE)
                    .build();
            } catch (Exception e) {
                log.error("Failed to parse config JSON for config id: {}", config.getId(), e);
                throw new RuntimeException("Failed to parse config JSON", e);
            }
        });
    }


    @Transactional(readOnly = true)
    public boolean isConfigValid(String etag) {
        return etag != null && repository.existsByHashAndIsActiveTrue(etag);
    }


    @Transactional
    public AppConfigBundleDto createConfig(CreateConfigRequest request, String createdBy) {
        log.info("Creating new config for app={}, platform={}, country={}",
                request.getApp(), request.getPlatform(), request.getCountry());

        AppConfigBundle entity = mapper.toEntity(request, createdBy);
        AppConfigBundle saved = repository.save(entity);

        log.info("Created config with id: {}", saved.getId());
        return mapper.toDto(saved);
    }


    @Transactional
    public AppConfigBundleDto updateConfig(Long configId, CreateConfigRequest request, String createdBy) {
        log.info("Updating config id={} for app={}, platform={}",
                configId, request.getApp(), request.getPlatform());

        AppConfigBundle existingConfig = repository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Config not found with id: " + configId));

        AppConfigBundle entity = mapper.toEntity(request, createdBy);
        entity.setId(configId);
        AppConfigBundle saved = repository.save(entity);

        log.info("Updated config with id: {}", saved.getId());
        return mapper.toDto(saved);
    }



    @Transactional
    public void deleteConfig(Long id) {
        AppConfigBundle entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Config not found with id: " + id));

        entity.setActive(false);
        repository.save(entity);

        log.info("Soft deleted config with id: {}", id);
    }


    @Transactional(readOnly = true)
    public AppConfigBundleDto getConfigById(Long id) {
        return repository.findById(id)
            .map(mapper::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Config not found with id: " + id));
    }


    @Transactional(readOnly = true)
    public List<AppConfigBundleDto> listConfigs(AppType app, PlatformType platform) {
        if (app != null && platform != null) {
            return repository.findByAppAndPlatformOrderByCreatedAtDesc(app, platform)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        } else {
            return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        }
    }


    @Transactional(readOnly = true)
    public List<AppConfigBundleDto> listAllActiveConfigs() {
        return repository.findByIsActiveTrueOrderByCreatedAtDesc()
            .stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }
}

