package com.example.app.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {
    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver("access_token");

    @Test
    void ignoresStaleAccessCookieOnPublicAuthenticationEndpoints() {
        for (String path : new String[]{"/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/auth/refresh"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setCookies(new Cookie("access_token", "expired-or-invalid"));
            assertThat(resolver.resolve(request)).isNull();
        }
    }

    @Test
    void resolvesAccessCookieOnProtectedEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.setCookies(new Cookie("access_token", "valid-token"));
        assertThat(resolver.resolve(request)).isEqualTo("valid-token");
    }
}
