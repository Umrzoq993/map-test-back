package com.agri.mapapp.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Service
public class JwtService {

    private final Key key;
    private final long accessExpMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-exp-minutes:120}") long accessExpMinutes, // default 120 daqiqa
            @Value("${app.jwt.issuer:mapapp}") String issuer
    ) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 uchun kamida 32 bayt kalit
        if (raw.length < 32) {
            raw = Arrays.copyOf(raw, 32);
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.accessExpMs = accessExpMinutes * 60_000L;
        this.issuer = issuer;
    }

    public String generateAccessToken(String username, Role role, Long orgId) {
        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        // App-ichki claimlar
        claims.put("role", role != null ? role.name() : "USER");
        if (orgId != null) claims.put("orgId", orgId);
        // Spring Security uchun authorities (ROLE_ prefiks bilan)
        String r = role != null ? role.name() : "USER";
        claims.put("authorities", List.of("ROLE_" + r));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessExpMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(UserPrincipal up) {
        return generateAccessToken(up.getUsername(), up.getRole(), up.getOrgId());
    }

    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public String getUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public Role getRole(String token) {
        String r = Objects.toString(parse(token).getBody().get("role"), "USER");
        try { return Role.valueOf(r); } catch (Exception e) { return Role.ORG_USER; }
    }

    public Long getOrgId(String token) {
        Object v = parse(token).getBody().get("orgId");
        if (v == null) return null;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l) return l;
        if (v instanceof String s) return Long.parseLong(s);
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthorities(String token) {
        Object arr = parse(token).getBody().get("authorities");
        if (arr instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) out.add(Objects.toString(o, ""));
            return out;
        }
        // fallback agar yo'q bo'lsa
        String r = Objects.toString(parse(token).getBody().get("role"), "USER");
        return List.of("ROLE_" + r);
    }
}
