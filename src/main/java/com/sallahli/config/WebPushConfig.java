package com.sallahli.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Web Push (VAPID) Configuration.
 * Supports browser push notifications for web clients.
 */
@Configuration
@ConfigurationProperties(prefix = "webpush")
@Data
@Slf4j
public class WebPushConfig {

    /**
     * Enable/disable web push notifications
     */
    private boolean enabled = true;

    /**
     * VAPID public key (Base64 encoded)
     * This key is shared with the frontend to subscribe to push notifications
     */
    private String vapidPublicKey = "BKzT3H3f2X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9";

    /**
     * VAPID private key (Base64 encoded)
     * Keep this secret - only used on server side
     */
    private String vapidPrivateKey = "YOUR_VAPID_PRIVATE_KEY_HERE";

    /**
     * Subject for VAPID (usually mailto: or https:// URL)
     */
    private String vapidSubject = "mailto:admin@sallahli.com";

    /**
     * Time-to-live for web push messages (in seconds)
     */
    private int ttl = 86400; // 24 hours

    public void logConfiguration() {
        if (enabled) {
            log.info("Web Push enabled. VAPID public key: {}...",
                    vapidPublicKey != null && vapidPublicKey.length() > 20
                            ? vapidPublicKey.substring(0, 20)
                            : "NOT_CONFIGURED");
        } else {
            log.info("Web Push is disabled");
        }
    }
}
