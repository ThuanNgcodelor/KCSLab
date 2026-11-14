package com.example.app.auth.controller;

import com.example.app.auth.dto.AuthUserResponse;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.service.AuthService;
import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final String cookiePath;
    private final boolean secureCookie;

    public AuthController(AuthService authService, UserService userService,
                          @Value("${app.security.cookie-path:/}") String cookiePath,
                          @Value("${app.security.secure-cookie:false}") boolean secureCookie) {
        this.authService = authService;
        this.userService = userService;
        this.cookiePath = cookiePath;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken token) {
        return ResponseEntity.ok(token);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        setCookie(response, "access_token", result.accessToken(), 900, true);
        setCookie(response, "refresh_token", result.refreshToken(), 30 * 24 * 60 * 60, true);
        return ResponseEntity.ok(toResponse(result.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthUserResponse> refresh(jakarta.servlet.http.HttpServletRequest request,
                                                     HttpServletResponse response) {
        String token = cookie(request, "refresh_token");
        AuthService.LoginResult result = authService.refresh(token);
        setCookie(response, "access_token", result.accessToken(), 900, true);
        setCookie(response, "refresh_token", result.refreshToken(), 30 * 24 * 60 * 60, true);
        return ResponseEntity.ok(toResponse(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request,
                                       HttpServletResponse response) {
        authService.logout(cookie(request, "refresh_token"));
        clearCookie(response, "access_token");
        clearCookie(response, "refresh_token");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getByEmail(jwt.getSubject());
        return ResponseEntity.ok(toResponse(user));
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                userService.avatarUrl(user));
    }

    private String cookie(jakarta.servlet.http.HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(cookiePath)
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        setCookie(response, name, "", 0, true);
    }
}
