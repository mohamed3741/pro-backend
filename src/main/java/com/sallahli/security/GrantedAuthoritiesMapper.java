package com.sallahli.security;

import com.sallahli.security.constant.KeycloakUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts JWT tokens to ClinicAuthenticationToken with clinic information.
 * Extracts clinic_id from Keycloak user attributes for multi-tenancy support.
 */
@Slf4j
public class GrantedAuthoritiesMapper implements Converter<Jwt, ClinicAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    public ClinicAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = extractAuthorities(jwt);
        String username = jwt.getClaimAsString(StandardClaimNames.PREFERRED_USERNAME);
        String email = jwt.getClaimAsString(StandardClaimNames.EMAIL);
        Long clinicId = extractClinicId(jwt);

        log.debug("Authenticated user: {}, clinicId: {}, authorities: {}", username, clinicId, authorities);

        return new ClinicAuthenticationToken(jwt, authorities, username, clinicId, email);
    }

    /**
     * Extract roles from the JWT realm_access claim.
     */
    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess == null || !realmAccess.containsKey(ROLES)) {
            log.warn("No realm_access.roles found in JWT");
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get(ROLES);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Extract clinic_id from JWT claims.
     * The clinic_id should be set as a user attribute in Keycloak and mapped to the token.
     *
     * In Keycloak Admin Console:
     * 1. Go to your realm -> Clients -> your-client -> Client scopes -> dedicated scope
     * 2. Add a mapper: User Attribute -> clinicId (camelCase attribute name)
     * 3. Set "Token Claim Name" to "clinic_id" (snake_case in JWT)
     * 4. Enable "Add to ID token" and "Add to access token"
     */
    private Long extractClinicId(Jwt jwt) {
        // Try to get clinic_id directly from claims
        Object clinicIdClaim = jwt.getClaim(KeycloakUtils.CLINIC_ID_JWT_CLAIM);
        
        if (clinicIdClaim != null) {
            return parseClinicId(clinicIdClaim);
        }

        log.debug("No clinic_id found in JWT for user: {}", 
                jwt.getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return null;
    }

    /**
     * Parse clinic_id from various possible types.
     */
    private Long parseClinicId(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Long longValue) {
            return longValue;
        }
        
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Invalid clinic_id format: {}", stringValue);
                return null;
            }
        }
        
        // Handle case where it's a list (Keycloak sometimes returns attributes as lists)
        if (value instanceof List<?> listValue && !listValue.isEmpty()) {
            return parseClinicId(listValue.get(0));
        }
        
        log.warn("Unexpected clinic_id type: {}", value.getClass().getName());
        return null;
    }
}


