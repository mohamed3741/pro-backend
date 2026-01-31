package com.sallahli.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                // Check class level if not on method
                preAuthorize = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
            }

            if (preAuthorize != null) {
                String expression = preAuthorize.value();
                // Include endpoints accessible to all authenticated users or public
                if (expression.contains("isAuthenticated()") || expression.contains("permitAll()")) {
                    return operation;
                }
                // If it requires a specific role, checks if our target role is present
                // Simple string check: if the expression contains the role name
                // This covers hasRole('ROLE'), hasAnyRole('ROLE1', 'ROLE2')
                if (expression.contains("'" + role + "'") || expression.contains("hasRole('" + role + "')")) {
                    return operation;
                }
                // If the annotation exists but does NOT contain our role, hide it
                // UNLESS it's generic like isAuthenticated() which might be too broad,
                // but for this specific requirement we assume role-based checks.
                // If we want to include public endpoints (no PreAuthorize), we handle that
                // below.
                return null;
            }

            // If no PreAuthorize annotation, it's public, so include it
            return operation;
        };
    }
}
