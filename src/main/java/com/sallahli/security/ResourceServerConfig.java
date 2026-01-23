package com.sallahli.security;

import com.sallahli.security.filters.AuthorizationFilter;
import com.sallahli.security.filters.TokenParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class ResourceServerConfig {

    private final TokenParser tokenParser;

    private final List<String> authorizedClientIds;

    private final String[] authorizedPatternUri;

    @Autowired
    public ResourceServerConfig(@Value("#{'${keycloak.authorized.client.ids:''}'.split(',')}") List<String> authorizedClientIds,
                                @Value("#{'${spring.security.authorized.uri.pattern:''}'.split(',')}") String[] authorizedPatternUri,
                                TokenParser tokenParser) {
        this.tokenParser = tokenParser;
        this.authorizedClientIds = authorizedClientIds;
        this.authorizedPatternUri = Arrays.stream(authorizedPatternUri).map(String::trim).toArray(String[]::new);;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        http.sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrfConfigurer -> csrfConfigurer.ignoringRequestMatchers(authorizedPatternUri))
            .authorizeHttpRequests((authorizeHttpRequests) ->
                    authorizeHttpRequests
                            // allow swagger and api docs
                            .requestMatchers(
                                    "/v3/api-docs",
                                    "/v3/api-docs/**",
                                    "/swagger-ui.html",
                                    "/swagger-ui/**"
                            ).permitAll()
                            .requestMatchers(authorizedPatternUri).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS).permitAll()
                            .requestMatchers("/**").authenticated()
                ).cors(conf -> conf.configurationSource(source))
                .addFilterAfter(new AuthorizationFilter(tokenParser, authorizedClientIds, authorizedPatternUri), AbstractPreAuthenticatedProcessingFilter.class)
                .oauth2ResourceServer(resourceServerConfigure -> {
                    resourceServerConfigure.jwt(jwtConfigurer -> {
                                jwtConfigurer.jwtAuthenticationConverter(new GrantedAuthoritiesMapper());
                            }
                    );
                });
        return http.build();
    }

}


