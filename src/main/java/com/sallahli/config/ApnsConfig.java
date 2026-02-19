package com.sallahli.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Configuration
@Slf4j
public class ApnsConfig {

    @Value("${apns.team-id:}")
    private String teamId;

    @Value("${apns.key-id:}")
    private String keyId;

    @Value("${apns.p8-file-base64:}")
    private String p8FileBase64;

    @Getter
    @Value("${apns.client-bundle:com.sallahli.client.stg}")
    private String clientBundle;

    @Getter
    @Value("${apns.pro-bundle:com.sallahli.pro.stg}")
    private String proBundle;

    @Value("${apns.production:false}")
    private boolean production;

    private boolean isConfigured() {
        return p8FileBase64 != null && !p8FileBase64.isBlank()
                && teamId != null && !teamId.isBlank()
                && keyId != null && !keyId.isBlank();
    }

    private ApnsClient buildApnsClient(String label) {
        if (!isConfigured()) {
            log.info("APNs {} is not configured (missing p8-file-base64, team-id, or key-id). Returning null.", label);
            return null;
        }

        try {
            byte[] p8Bytes = Base64.getDecoder().decode(p8FileBase64);

            ApnsSigningKey signingKey = ApnsSigningKey.loadFromInputStream(
                    new ByteArrayInputStream(p8Bytes),
                    teamId,
                    keyId);

            ApnsClientBuilder builder = new ApnsClientBuilder();

            if (production) {
                builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
            } else {
                builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
            }

            ApnsClient client = builder
                    .setSigningKey(signingKey)
                    .build();

            log.info("APNs {} client initialized (production: {})", label, production);
            return client;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialize APNs {} client: {}", label, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 for APNs p8 key: {}", e.getMessage());
            return null;
        }
    }

    @Bean(name = "clientApnsClient")
    public ApnsClient clientApnsClient() {
        ApnsClient client = buildApnsClient("client");
        if (client != null) {
            log.info("APNs client initialized for bundle: {}", clientBundle);
        }
        return client;
    }

    @Bean(name = "proApnsClient")
    public ApnsClient proApnsClient() {
        ApnsClient client = buildApnsClient("pro");
        if (client != null) {
            log.info("APNs client initialized for bundle: {}", proBundle);
        }
        return client;
    }
}
