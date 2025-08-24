// src/main/java/com/agri/mapapp/bootstrap/DevUserSeeder.java
package com.agri.mapapp.bootstrap;

import com.agri.mapapp.auth.AppUser;
import com.agri.mapapp.auth.AppUserRepository;
import com.agri.mapapp.auth.Role;
import com.agri.mapapp.auth.UserStatus;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DevUserSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final OrganizationUnitRepository orgUnits;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ixtiyoriy: mavjud org unitlardan birini "default" sifatida ulab yuboramiz.
        OrganizationUnit anyUnit = null;
        try {
            anyUnit = orgUnits.findAll().stream().findFirst().orElse(null);
        } catch (Exception ignore) {
            // org bo'limlari bo'lmasa ham davom etaveramiz (null bo'lishi mumkin)
        }

        seedAdmin(anyUnit);
        seedUser(anyUnit);
    }

    private void seedAdmin(OrganizationUnit unit) {
        Optional<AppUser> ex = users.findByUsername("admin");
        if (ex.isPresent()) {
            AppUser u = ex.get();
            // Schema yangilanishidan so'ng muvofiqlashtirib qo'yamiz
            if (u.getFullName() == null || u.getFullName().isBlank()) u.setFullName("Administrator");
            if (u.getStatus() == null) u.setStatus(UserStatus.ACTIVE);
            if (u.getRole() == null) u.setRole(Role.ADMIN);
            // Eski koddagi setOrg(...) o'rniga:
            if (u.getOrgUnit() == null && unit != null) u.setOrgUnit(unit);
            users.save(u);
            log.info("Admin foydalanuvchi mavjud: {}", u.getUsername());
            return;
        }

        AppUser admin = AppUser.builder()
                .username("admin")
                .password(encoder.encode("admin123")) // dev uchun
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .fullName("Administrator")
                .position("Administrator")
                .title(null)
                .phone(null)
                .avatarUrl(null)
                .department(null)
                .orgUnit(unit) // yangi bog'lanish: OrganizationUnit
                .build();

        users.save(admin);
        log.info("Admin foydalanuvchi yaratildi: admin / admin123");
    }

    private void seedUser(OrganizationUnit unit) {
        Optional<AppUser> ex = users.findByUsername("user");
        if (ex.isPresent()) {
            AppUser u = ex.get();
            if (u.getFullName() == null || u.getFullName().isBlank()) u.setFullName("Oddiy foydalanuvchi");
            if (u.getStatus() == null) u.setStatus(UserStatus.ACTIVE);
            if (u.getRole() == null) u.setRole(Role.USER);
            if (u.getOrgUnit() == null && unit != null) u.setOrgUnit(unit);
            users.save(u);
            log.info("User foydalanuvchi mavjud: {}", u.getUsername());
            return;
        }

        AppUser user = AppUser.builder()
                .username("user")
                .password(encoder.encode("user123")) // dev uchun
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .fullName("Oddiy foydalanuvchi")
                .position(null)
                .title(null)
                .phone(null)
                .avatarUrl(null)
                .department(null)
                .orgUnit(unit)
                .build();

        users.save(user);
        log.info("User foydalanuvchi yaratildi: user / user123");
    }
}
