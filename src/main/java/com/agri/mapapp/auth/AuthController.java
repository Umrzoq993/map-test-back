package com.agri.mapapp.auth;

import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationUnitRepository orgRepo;

    // ================== LOGIN (mavjud) ==================
    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@RequestBody LoginReq req) {
        Authentication a = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        var principal = (UserPrincipal) a.getPrincipal();
        String token = jwt.generateToken(principal.getUsername(), principal.getRole(), principal.getOrgId());
        return ResponseEntity.ok(new TokenRes(token));
    }

    // ================== BIRINCHI ADMIN ==================
    // DB da hali birorta user bo'lmasa, admin yaratadi. Keyin bu endpoint 403 qaytaradi.
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody BootstrapAdminReq req) {
        if (users.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Msg("Bootstrap disabled: users already exist"));
        }
        AppUser admin = AppUser.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.ADMIN)
                .org(null) // admin uchun majburiy emas
                .build();
        users.save(admin);
        return ResponseEntity.ok(new Msg("Admin user created"));
    }

    // ================== ADMIN -> USER CREATE ==================
    // ADMIN token bilan yangi foydalanuvchi yaratadi (ADMIN yoki ORG_USER).
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserReq req) {
        if (users.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Msg("Username already exists"));
        }
        OrganizationUnit org = null;
        if (req.getOrgId() != null) {
            org = orgRepo.findById(req.getOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found: id=" + req.getOrgId()));
        }
        Role role = req.getRole() == null ? Role.ORG_USER : req.getRole();

        AppUser u = AppUser.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .org(org)
                .build();
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Msg("User created"));
    }

    // ================== DTOs ==================
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginReq { private String username; private String password; }

    @Getter @AllArgsConstructor
    public static class TokenRes { private String token; }

    @Getter @Setter
    public static class BootstrapAdminReq {
        private String username;
        private String password;
    }

    @Getter @Setter
    public static class CreateUserReq {
        private String username;
        private String password;
        private Role role;      // ADMIN yoki ORG_USER
        private Long orgId;     // ixtiyoriy (ORG_USER uchun)
    }

    @Getter @AllArgsConstructor
    public static class Msg { private String message; }
}
