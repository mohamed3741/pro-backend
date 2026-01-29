package com.sallahli.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Apple Push Notification Service (APNs) Configuration.
 * Supports iOS push notifications for both Client and Pro apps.
 */
@Configuration
@Slf4j
public class ApnsConfig {

    // ========================================================================
    // Client App Configuration (iOS)
    // ========================================================================

    @Value("${apns.client.enabled:true}")
    private boolean clientApnsEnabled;

    @Value("${apns.client.key-file:apns-client-key.p8}")
    private String clientKeyFile;

    @Value("${apns.client.key-id:XXXXXXXXXX}")
    private String clientKeyId;

    @Value("${apns.client.team-id:XXXXXXXXXX}")
    private String clientTeamId;

    @Value("${apns.client.bundle-id:com.sallahli.client}")
    private String clientBundleId;

    // ========================================================================
    // Pro App Configuration (iOS)
    // ========================================================================

    @Value("${apns.pro.enabled:true}")
    private boolean proApnsEnabled;

    @Value("${apns.pro.key-file:apns-pro-key.p8}")
    private String proKeyFile;

    @Value("${apns.pro.key-id:YYYYYYYYYY}")
    private String proKeyId;

    @Value("${apns.pro.team-id:YYYYYYYYYY}")
    private String proTeamId;

    @Value("${apns.pro.bundle-id:com.sallahli.pro}")
    private String proBundleId;

    // ========================================================================
    // Common Configuration
    // ========================================================================

    @Value("${apns.production:false}")
    private boolean production;

    @Bean(name = "clientApnsClient")
    public ApnsClient clientApnsClient() {
        if (!clientApnsEnabled) {
            log.info("Client APNs is disabled. Returning null.");
            return null;
        }

        try {
            ApnsClientBuilder builder = new ApnsClientBuilder();

            if (production) {
                builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
            } else {
                builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
            }

            ApnsSigningKey signingKey = ApnsSigningKey.loadFromInputStream(
                    new ClassPathResource(clientKeyFile).getInputStream(),
                    clientTeamId,
                    clientKeyId);

            ApnsClient client = builder
                    .setSigningKey(signingKey)
                    .build();

            log.info("Client APNs client initialized for bundle: {} (production: {})", clientBundleId, production);
            return client;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialize Client APNs client: {}", e.getMessage());
            return null;
        }
    }

    @Bean(name = "proApnsClient")
    public ApnsClient proApnsClient() {
        if (!proApnsEnabled) {
            log.info("Pro APNs is disabled. Returning null.");
            return null;
        }

        try {
            ApnsClientBuilder builder = new ApnsClientBuilder();

            if (production) {
                builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
            } else {
                builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
            }

            ApnsSigningKey signingKey = ApnsSigningKey.loadFromInputStream(
                    new ClassPathResource(proKeyFile).getInputStream(),
                    proTeamId,
                    proKeyId);

            ApnsClient client = builder
                    .setSigningKey(signingKey)
                    .build();

            log.info("Pro APNs client initialized for bundle: {} (production: {})", proBundleId, production);
            return client;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialize Pro APNs client: {}", e.getMessage());
            return null;
        }
    }

    // Getters for bundle IDs (used by PushNotificationService)
    public String getClientBundleId() {
        return clientBundleId;
    }

    public String getProBundleId() {
        return proBundleId;
    }
}
