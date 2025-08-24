package com.agri.mapapp.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl uds;
    private static final AntPathMatcher PM = new AntPathMatcher();

    private boolean isWhitelisted(String path) {
        // faqat quyidagi yo'llarni whitelist qilamiz (boshqa /api/auth/** emas!)
        return PM.match("/api/auth/login", path) ||
                PM.match("/api/auth/refresh", path) ||
                PM.match("/api/auth/logout", path) ||
                PM.match("/swagger-ui/**", path) ||
                PM.match("/v3/api-docs/**", path) ||
                PM.match("/api-docs/**", path) ||
                PM.match("/swagger-ui.html", path) ||
                PM.match("/actuator/health", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        // CORS preflight – autentifikatsiya talab qilmaymiz
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String path = req.getRequestURI();
        if (isWhitelisted(path)) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                if (jwtService.isValid(token) &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    Claims c = jwtService.parse(token).getBody();
                    String username = c.getSubject();

                    // Token-dagi authorities claim
                    List<String> auths = jwtService.getAuthorities(token);
                    var tokenAuthorities = auths.stream()
                            .filter(s -> s != null && !s.isBlank())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Agar xohlasangiz, DB'dan ham tekshirish:
                    var ud = (UserPrincipal) uds.loadUserByUsername(username);
                    // Token authorities ustuvor; DB dagi rollarni qo'shib yuborish ham mumkin:
                    if (tokenAuthorities.isEmpty()) {
                        tokenAuthorities = ud.getAuthorities().stream()
                                .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                                .collect(Collectors.toList());
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(
                            ud, null, tokenAuthorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // token invalid -> unauthenticated
            }
        }

        chain.doFilter(req, res);
    }
}
