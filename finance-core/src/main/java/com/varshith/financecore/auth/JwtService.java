package com.varshith.financecore.auth;

import com.varshith.financecore.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long ttlSeconds;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.encoder = encoder;
        this.ttlSeconds = ttlSeconds;
    }

    public long getTtlSeconds() { return ttlSeconds; }

    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("finance-core")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim("roles", List.of(user.getRole()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}