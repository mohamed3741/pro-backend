package com.sallahli.security.filters;

import com.sallahli.security.constant.KeycloakUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TokenParser {


    public boolean isAuthorized(HttpServletRequest request, List<String> authorizedClientIds) {


        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return true ;
        }

        String token = bearerToken.replace("Bearer", ""); // Supprimer "Bearer " pour obtenir le JWT
        SignedJWT signedJWT = null;
        try {
            signedJWT = SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        JWTClaimsSet claimsSet= null;
        try {
            claimsSet = signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Map<String,Object> claims =claimsSet.getClaims();
        String clientIdName = (String) claims.get(KeycloakUtils.CLAIM_CLIENT_ID);
        if(authorizedClientIds.contains(clientIdName)){
            return true;
        }else {
            return Boolean.parseBoolean((String) claims.get(KeycloakUtils.CLAIM_ACCOUNT_ACTIVE));
        }
    }
}


