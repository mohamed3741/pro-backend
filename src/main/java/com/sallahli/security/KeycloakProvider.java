package com.sallahli.security;

import ae.lambdabeta.common.security.constant.LoginProvider;
import ae.lambdabeta.common.security.constant.RefreshTokenDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.Unirest;
import lombok.Getter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@ConditionalOnProperty(
    value="keycloak.admin-client.enabled", 
    havingValue = "true", 
    matchIfMissing = false)
public class KeycloakProvider {

    @Value("${keycloak.auth-server-url}")
    public String serverURL;
    @Value("${keycloak.realm}")
    public String realm;
    @Value("${keycloak.client-id}")
    public String clientID;
    @Value("${keycloak.credentials.secret}")
    public String clientSecret;

    ObjectMapper mapper = new ObjectMapper();

    public KeycloakProvider() {
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .realm(realm)
                .serverUrl(serverURL)
                .clientId(clientID)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();

    }


    public KeycloakBuilder newKeycloakBuilderWithPasswordCredentials(String username, String password) {
        return KeycloakBuilder.builder()
                .realm(realm)
                .serverUrl(serverURL)
                .clientId(clientID)
                .clientSecret(clientSecret)
                .scope("openid profile email phone")
                .username(username)
                .password(password);
    }

    public AccessTokenResponse exchangeToken(String exchangeableToken, LoginProvider provider) throws JsonProcessingException {
        String url = serverURL + "/realms/" + realm + "/protocol/openid-connect/token";
        String response = Unirest
                .post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("client_id", clientID)
                .field("client_secret", clientSecret)
                .field("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                .field("subject_token", exchangeableToken)
                .field("subject_token_type", provider.getSubjectTokenType())
                .field("subject_issuer", provider.getProviderName())
                .asJson().getBody().toString();

        //TODO parse from unirest directement
        return mapper.readValue(response, AccessTokenResponse.class);
    }

    public AccessTokenResponse exchangeTokenImpersonation(String exchangeableToken, String userId) throws JsonProcessingException {
        String url = serverURL + "/realms/" + realm + "/protocol/openid-connect/token";
        String response = Unirest
                .post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("client_id", clientID)
                .field("client_secret", clientSecret)
                .field("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                .field("subject_token", exchangeableToken)
                .field("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
                .field("requested_subject", userId)
                .asJson().getBody().toString();

        //TODO parse from unirest directement
        return mapper.readValue(response, AccessTokenResponse.class);
    }

    public AccessTokenResponse refreshToken(RefreshTokenDto refreshToken) throws JsonProcessingException {
        String url = serverURL + "/realms/" + realm + "/protocol/openid-connect/token";
        String response = Unirest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("client_id", clientID)
                .field("client_secret", clientSecret)
                .field("refresh_token", refreshToken.getRefreshToken())
                .field("grant_type", "refresh_token")
                .asJson().getBody().toString();

        //TODO parse from unirest directement
        return mapper.readValue(response, AccessTokenResponse.class);
    }
}


