// src/main/java/com/agri/mapapp/bootstrap/DevUserSeeder.java
package com.agri.mapapp.bootstrap;

import com.agri.mapapp.auth.AppUser;
import com.agri.mapapp.auth.AppUserRepository;
import com.agri.mapapp.auth.Role;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DevUserSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final OrganizationUnitRepository orgs;
    private final PasswordEncoder encoder;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (users.count() > 0) return;

        // Admin (global)
        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setOrg(null);
        users.save(admin);

        // Ikkita filial topib, org userlar tayinlaymiz
        var all = orgs.findAllByOrderBySortOrderAscNameAsc();
        OrganizationUnit root = all.stream().filter(o -> o.getParent() == null).findFirst().orElse(null);
        var branches = all.stream().filter(o -> o.getParent() != null && o.getParent().getId().equals(root.getId())).limit(2).toList();

        for (int i = 0; i < branches.size(); i++) {
            var ou = branches.get(i);
            AppUser u = new AppUser();
            u.setUsername("filial" + (i + 1));
            u.setPassword(encoder.encode("user123"));
            u.setRole(Role.ORG_USER);
            u.setOrg(ou);
            users.save(u);
        }

        System.out.println("DevUserSeeder: admin/admin123 va filial1/user123, filial2/user123 yaratildi.");
    }
}
