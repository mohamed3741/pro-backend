package com.sallahli.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

public class GrantedAuthoritiesMapper implements Converter<Jwt, JwtAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        List<String> roles = (ArrayList) jwt.getClaimAsMap(REALM_ACCESS).get(ROLES);
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Add both ROLE_ prefixed (for hasRole) and non-prefixed (for hasAuthority)
        // versions
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
            authorities.add(new SimpleGrantedAuthority(role));
        }
        String name = jwt.getClaimAsString(StandardClaimNames.PREFERRED_USERNAME);
        return new JwtAuthenticationToken(jwt, authorities, name);
    }
}
