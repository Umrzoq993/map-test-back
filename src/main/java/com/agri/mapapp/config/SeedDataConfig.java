package com.agri.mapapp.config;

import com.agri.mapapp.auth.AppUser;
import com.agri.mapapp.auth.AppUserRepository;
import com.agri.mapapp.auth.Role;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Configuration
public class SeedDataConfig {

    @Bean
    public org.springframework.boot.CommandLineRunner seedUsers(
            AppUserRepository users,
            OrganizationUnitRepository orgs,
            PasswordEncoder pe
    ) {
        return args -> {
            // 1) Root org (agar yo'q bo'lsa) — ORG_USER uchun kerak
            OrganizationUnit root = findOrCreateRoot(orgs);

            // 2) ADMIN (barcha huquqlar bilan)
            if (users.findByUsername("admin").isEmpty()) {
                AppUser admin = AppUser.builder()
                        .username("admin")
                        .password(pe.encode("admin123")) // 👉 login: admin / admin123
                        .role(Role.ADMIN)
                        .org(null) // ADMIN uchun org ixtiyoriy
                        .build();
                users.save(admin);
                System.out.println("[seed] created ADMIN user: admin / admin123");
            }

            // 3) Oddiy org foydalanuvchisi (faqat o'zi va quyi bo'limlari)
            if (users.findByUsername("operator").isEmpty()) {
                AppUser operator = AppUser.builder()
                        .username("operator")
                        .password(pe.encode("operator123")) // 👉 login: operator / operator123
                        .role(Role.ORG_USER)
                        .org(root) // shu org va barcha quyi bo'limlari doirasida ishlaydi
                        .build();
                users.save(operator);
                System.out.println("[seed] created ORG_USER: operator / operator123 (org=" + root.getName() + ")");
            }
        };
    }

    private OrganizationUnit findOrCreateRoot(OrganizationUnitRepository orgs) {
        // Parent = null bo'lgan birinchi tugunni "root" deb olamiz
        OrganizationUnit root = null;
        try {
            // Agar sizda bunday query metodi bo'lsa:
            // List<OrganizationUnit> roots = orgs.findByParentIsNull();
            // root = roots.isEmpty() ? null : roots.get(0);

            // Hozirgi repo’da yo‘q, shuning uchun umumiy ro‘yxatdan izlab olamiz:
            List<OrganizationUnit> all = orgs.findAll();
            for (OrganizationUnit u : all) {
                if (u.getParent() == null) { root = u; break; }
            }
        } catch (Exception ignored) {}

        if (root != null) return root;

        // Hech biri topilmasa – yangi root yaratamiz
        OrganizationUnit created = OrganizationUnit.builder()
                .name("Root Farm")
                .sortOrder(0)
                .build();
        return orgs.save(created);
    }
}
