package com.sallahli.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${fcm.firebase-configuration-content-base64:}")
    private String firebaseConfigBase64;

    @PostConstruct
    public void initialize() {
        if (firebaseConfigBase64 == null || firebaseConfigBase64.isBlank()) {
            log.info("Firebase config base64 is empty. Skipping Firebase initialization.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                byte[] configBytes = Base64.getDecoder().decode(firebaseConfigBase64);
                ByteArrayInputStream serviceAccount = new ByteArrayInputStream(configBytes);

                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase application initialized successfully from base64 config.");
            } else {
                log.info("Firebase application already initialized.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 for Firebase config: {}. Push notifications will not work.", e.getMessage());
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}. Push notifications will not work.", e.getMessage());
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Returning null FirebaseMessaging bean.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
