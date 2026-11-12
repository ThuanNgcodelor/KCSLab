package com.example.app.auth.service;

import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenSeconds;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.security.issuer}") String issuer,
                        @Value("${app.security.access-token-seconds:900}") long accessTokenSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenSeconds))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
