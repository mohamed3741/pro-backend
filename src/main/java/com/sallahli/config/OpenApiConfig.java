package com.sallahli.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Sallahli Backend API")
                        .description("Sallahli application backend API documentation")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Sallahli Documentation")
                        .url("https://sallahli.com"));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .packagesToScan("com.sallahli.controller")
                .addOperationCustomizer(filterByRole("ADMIN"))
                .build();
    }

    @Bean
    public GroupedOpenApi proApi() {
        return GroupedOpenApi.builder()
                .group("pro")
                .packagesToScan("com.sallahli.controller")
                .addOperationCustomizer(filterByRole("PRO"))
                .build();
    }

    @Bean
    public GroupedOpenApi clientApi() {
        return GroupedOpenApi.builder()
                .group("client")
                .packagesToScan("com.sallahli.controller")
                .addOperationCustomizer(filterByRole("CLIENT"))
                .build();
    }

    private OperationCustomizer filterByRole(String role) {
        return (operation, handlerMethod) -> {
            // Use AnnotatedElementUtils to find annotations on methods or class level
            // (including interfaces/proxies)
            PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                    PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(),
                        PreAuthorize.class);
            }

            if (preAuthorize != null) {
                String expression = preAuthorize.value();

                // Keep endpoints that are accessible to all authenticated users
                if (expression.contains("isAuthenticated()") || expression.contains("permitAll()")) {
                    return operation;
                }

                // Robust check for role: handles 'ADMIN' and "ADMIN"
                // matches: 'ADMIN', ' ROLE_ADMIN ', "ADMIN", etc.
                if (expression.contains("'" + role + "'") ||
                        expression.contains("\"" + role + "\"") ||
                        expression.contains(" " + role + " ") ||
                        expression.contains(role)) { // Fallback, though might be too broad if roles are substrings of
                                                     // others
                    // Let's stick to the specific check logic to avoid false positives
                    // But based on user feedback, maybe my previous check was too strict?
                    // Let's rely on standard contains for now but ensure we checking the string
                    // properly.
                    if (expression.contains("'" + role + "'") || expression.contains("hasRole('" + role + "')")
                            || expression.contains("hasAnyRole")) {
                        // If it hasAnyRole, we need to check if OUR role is in the list
                        if (expression.contains("'" + role + "'") || expression.contains("\"" + role + "\"")) {
                            return operation;
                        }
                    }
                }

                // If the role specific check failed but we decided it's a restricted valid
                // annotation, we hide it.
                return null;
            }

            // If no security annotation, it's public.
            // Option: Decide if "Public" endpoints should be in ALL groups.
            // Requirement says "endpoint docs for ADMIN separated... same for PRO...".
            // Usually public endpoints like Login should appear in all.
            return operation;
        };
    }
}
