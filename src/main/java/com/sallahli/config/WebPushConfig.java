package com.sallahli.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "webpush")
@Data
@Slf4j
public class WebPushConfig {

    
    private boolean enabled = true;

    
    private String vapidPublicKey = "BKzT3H3f2X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9X9";

    
    private String vapidPrivateKey = "YOUR_VAPID_PRIVATE_KEY_HERE";

    
    private String vapidSubject = "mailto:admin@sallahli.com";

    
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
