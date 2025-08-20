package com.agri.mapapp.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    // TODO: application.yml dan oling
    private static final String SECRET = "change-this-very-long-secret-key-please-32bytes-min!!!";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXP_MS = 1000L * 60 * 60 * 8; // 8 soat

    public String generateToken(String username, Role role, Long orgId) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role.name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXP_MS))
                .signWith(key, SignatureAlgorithm.HS256);

        // orgId null bo'lsa claim qo'shmang!
        if (orgId != null) {
            builder.claim("orgId", orgId);
        }
        return builder.compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
