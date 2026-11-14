package com.example.app.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.Set;

public class CookieBearerTokenResolver implements BearerTokenResolver {
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/auth/refresh");
    private final String cookieName;
    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    public CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (PUBLIC_AUTH_PATHS.contains(request.getRequestURI())) return null;
        String headerToken = headerResolver.resolve(request);
        if (headerToken != null) return headerToken;
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
