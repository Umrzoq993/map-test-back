package com.agri.mapapp.auth;

import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager am;
    private final JwtService jwt;
    private final AppUserRepository users;
    private final PasswordEncoder pe;

    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@RequestBody LoginReq req) {
        Authentication a = am.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        var principal = (UserPrincipal) a.getPrincipal();

        String token = jwt.generateToken(principal.getUsername(), principal.getRole(), principal.getOrgId());
        return ResponseEntity.ok(new TokenRes(token));
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginReq { private String username; private String password; }

    @Getter @AllArgsConstructor
    public static class TokenRes { private String token; }
}
