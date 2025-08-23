// src/main/java/com/agri/mapapp/bootstrap/FacilityDevSeeder.java
package com.agri.mapapp.bootstrap;

import com.agri.mapapp.facility.*;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class FacilityDevSeeder implements ApplicationRunner {

    private final OrganizationUnitRepository orgs;
    private final FacilityRepository facilities;
    private final ObjectMapper om;
    private final Random rnd = new Random(72);

    // Ko‘proq turlar — dashboard bo‘limlarining hammasi ko‘rinsin
    private static final List<FacilityType> TYPES = List.of(
            FacilityType.GREENHOUSE,
            FacilityType.FISHPOND, FacilityType.FISHFARM,
            FacilityType.COWSHED, FacilityType.SHEEPFOLD, FacilityType.STABLE,
            FacilityType.POULTRY, FacilityType.POULTRY_EGG, FacilityType.POULTRY_MEAT, FacilityType.TURKEY,
            FacilityType.WORKSHOP, FacilityType.WORKSHOP_COOKIE, FacilityType.WORKSHOP_SAUSAGE,
            FacilityType.WAREHOUSE,
            FacilityType.ORCHARD, FacilityType.FIELD, FacilityType.APIARY,
            FacilityType.AUX_LAND, FacilityType.BORDER_LAND
    );

    @Override
    public void run(ApplicationArguments args) {
        if (facilities.count() > 0) return;

        var all = orgs.findAllByOrderBySortOrderAscNameAsc();
        // leaf = bo‘lim: parent bor, ammo uning bolasi yo‘q
        var leaves = all.stream()
                .filter(o -> o.getParent() != null)
                .filter(o -> !orgs.existsByParent(o))
                .toList();

        long created = 0;
        for (OrganizationUnit leaf : leaves) {
            int n = 5 + rnd.nextInt(5); // 5..9 ta obyekt
            for (int i = 0; i < n; i++) {
                Facility f = new Facility();
                f.setOrg(leaf);

                FacilityType type = TYPES.get(rnd.nextInt(TYPES.size()));
                f.setType(type);

                // Status tarqalishi: 60% ACTIVE, 25% INACTIVE, 15% UNDER_MAINTENANCE
                int s = rnd.nextInt(100);
                if (s < 60) f.setStatus(FacilityStatus.ACTIVE);
                else if (s < 85) f.setStatus(FacilityStatus.INACTIVE);
                else f.setStatus(FacilityStatus.UNDER_MAINTENANCE);

                f.setName(leaf.getName() + " — " + type.name() + " #" + (i + 1));

                // Koordinata: org markazi atrofida kichik shovqin
                double baseLat = leaf.getLat() != null ? leaf.getLat() : 41.3111;
                double baseLng = leaf.getLng() != null ? leaf.getLng() : 69.2797;
                f.setLat(baseLat + (rnd.nextDouble() - 0.5) * 0.03);
                f.setLng(baseLng + (rnd.nextDouble() - 0.5) * 0.03);
                f.setZoom(leaf.getZoom() != null ? leaf.getZoom() : 12);

                // Attributes — turiga qarab foydali maydonlar
                ObjectNode a = om.createObjectNode();
                a.put("note", "Demo");
                a.put("updated_by", "Seeder");
                switch (type) {
                    case GREENHOUSE -> {
                        a.put("area_m2", 500 + rnd.nextInt(2500));
                        a.put("crop", rnd.nextBoolean() ? "Tomat" : "Bodring");
                    }
                    case FISHPOND, FISHFARM -> {
                        a.put("water_area_ha", Math.round((0.5 + rnd.nextDouble() * 9) * 10) / 10.0);
                        a.put("species", rnd.nextBoolean() ? "Karp" : "Qorabalik");
                    }
                    case COWSHED -> {
                        a.put("cows", 20 + rnd.nextInt(180));
                        a.put("milk_tpd", Math.round((0.5 + rnd.nextDouble() * 3) * 10) / 10.0);
                    }
                    case SHEEPFOLD -> a.put("sheep", 50 + rnd.nextInt(500));
                    case STABLE -> a.put("horses", 5 + rnd.nextInt(25));
                    case POULTRY, POULTRY_EGG, POULTRY_MEAT, TURKEY -> {
                        a.put("birds", 200 + rnd.nextInt(1800));
                        a.put("feed_tpm", Math.round((2 + rnd.nextDouble() * 18) * 10) / 10.0);
                    }
                    case WORKSHOP, WORKSHOP_COOKIE, WORKSHOP_SAUSAGE -> {
                        a.put("capacity_tpd", Math.round((1 + rnd.nextDouble() * 29) * 10) / 10.0);
                        a.put("certified", rnd.nextBoolean());
                    }
                    case WAREHOUSE -> a.put("storage_tons", 100 + rnd.nextInt(900));
                    case ORCHARD -> {
                        a.put("trees", 200 + rnd.nextInt(3000));
                        a.put("fruit", rnd.nextBoolean() ? "Olma" : "O‘rik");
                    }
                    case FIELD, AUX_LAND, BORDER_LAND -> a.put("area_ha", Math.round((2 + rnd.nextDouble() * 50) * 10) / 10.0);
                    case APIARY -> a.put("hives", 20 + rnd.nextInt(120));
                    default -> {}
                }
                f.setAttributes(a);

                // Vaqt maydonlari bo‘lsa to‘ldiramiz
                f.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(45)));
                f.setUpdatedAt(LocalDateTime.now().minusDays(rnd.nextInt(15)));

                facilities.save(f);
                created++;
            }
        }
        System.out.println("FacilityDevSeeder: " + created + " ta demo obyekt yaratildi.");
    }
}
