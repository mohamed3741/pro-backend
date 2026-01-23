package com.pro.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;

public class AuthorizationFilter extends AbstractPreAuthenticatedProcessingFilter {

    private final TokenParser tokenParser;
    private final List<String> authorizedClientIds;
    private final String[] authorizedPatternUri;

    private AntPathMatcher antPathMatcher;

    @Autowired
    public AuthorizationFilter(TokenParser tokenParser, List<String> authorizedClientIds, String[] authorizedPatternUri) {
        this.tokenParser = tokenParser;
        this.authorizedClientIds = authorizedClientIds;
        this.authorizedPatternUri = authorizedPatternUri;
        antPathMatcher = new AntPathMatcher();
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getPrincipal();
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getCredentials();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request;

        if (!authorizedUri(req) && !tokenParser.isAuthorized( req, authorizedClientIds)) {
            //Account is not active, you can choose how to handle this (e.g., return a 403 Forbidden response)
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is not authorized");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean authorizedUri(HttpServletRequest req) {

        for(String pathPattern : authorizedPatternUri){
            if(antPathMatcher.match(pathPattern.trim(), req.getRequestURI())) {
                return true;
            }
        }
        return false;
    }
}


