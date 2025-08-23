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

    // Faqat amaldagi turlar
    private static final List<FacilityType> TYPES = List.of(
            FacilityType.GREENHOUSE,
            FacilityType.POULTRY_EGG,
            FacilityType.POULTRY_MEAT,
            FacilityType.TURKEY,
            FacilityType.COWSHED,
            FacilityType.SHEEPFOLD,
            FacilityType.WORKSHOP_COOKIE,
            FacilityType.WORKSHOP_SAUSAGE,
            FacilityType.AUX_LAND,
            FacilityType.BORDER_LAND,
            FacilityType.FISHPOND
    );

    @Override
    public void run(ApplicationArguments args) {
        if (facilities.count() > 0) return;

        var all = orgs.findAllByOrderBySortOrderAscNameAsc();
        var leaves = all.stream()
                .filter(o -> o.getParent() != null)     // ildiz emas
                .filter(o -> !orgs.existsByParent(o))   // bolasi yo‘q => leaf
                .toList();

        long created = 0;
        for (OrganizationUnit leaf : leaves) {
            int n = 5 + rnd.nextInt(5); // 5..9 ta obyekt
            for (int i = 0; i < n; i++) {
                Facility f = new Facility();
                f.setOrg(leaf);

                FacilityType type = TYPES.get(rnd.nextInt(TYPES.size()));
                f.setType(type);

                // Status taqsimoti
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

                // Attributes — validator’ga mos kalitlar
                ObjectNode a = om.createObjectNode();
                a.put("note", "Demo");
                a.put("updated_by", "Seeder");

                switch (type) {
                    case GREENHOUSE -> {
                        a.put("totalAreaHa", round1(0.5 + rnd.nextDouble() * 5)); // ga
                        a.put("heatingType", rnd.nextBoolean() ? "Gaz" : "Qozon");
                        a.put("expectedYield", round1(5 + rnd.nextDouble() * 95)); // tonna/yil
                        a.put("expectedRevenue", round1(50_000_000 + rnd.nextDouble() * 300_000_000));
                        a.put("netProfit", round1(10_000_000 + rnd.nextDouble() * 100_000_000));
                    }
                    case POULTRY_EGG, POULTRY_MEAT, COWSHED, TURKEY, SHEEPFOLD -> {
                        a.put("areaM2", rnd.nextInt(3000) + 300); // m²
                        a.put("capacity", 100 + rnd.nextInt(3900));
                        a.put("current", 50 + rnd.nextInt(2000));
                        a.put("productAmount", round1(100 + rnd.nextDouble() * 5000)); // kg yoki dona
                        // Birlikni tovuq uchun ma’noli qilsak:
                        if (type == FacilityType.POULTRY_EGG) a.put("productUnit", "pcs");
                        else a.put("productUnit", "kg");
                        a.put("expectedRevenue", round1(20_000_000 + rnd.nextDouble() * 200_000_000));
                        a.put("netProfit", round1(5_000_000 + rnd.nextDouble() * 80_000_000));
                    }
                    case WORKSHOP_COOKIE, WORKSHOP_SAUSAGE -> {
                        a.put("areaM2", rnd.nextInt(2000) + 200);
                        a.put("productAmount", round1(1000 + rnd.nextDouble() * 10000)); // dona/oy yoki kg/oy
                        a.put("expectedRevenue", round1(40_000_000 + rnd.nextDouble() * 400_000_000));
                        a.put("netProfit", round1(8_000_000 + rnd.nextDouble() * 120_000_000));
                    }
                    case AUX_LAND, BORDER_LAND -> {
                        a.put("areaM2", rnd.nextInt(30000) + 5000); // m²
                        a.put("expectedYield", round1(500 + rnd.nextDouble() * 5000));
                        a.put("expectedRevenue", round1(10_000_000 + rnd.nextDouble() * 150_000_000));
                        a.put("netProfit", round1(3_000_000 + rnd.nextDouble() * 60_000_000));
                        a.put("tenant", rnd.nextBoolean() ? "MChJ Zamin Agro" : "MChJ Baraka Agro");
                        a.put("govDecision", "№" + (100 + rnd.nextInt(900)) + "/2024");
                    }
                    case FISHPOND -> {
                        a.put("areaM2", rnd.nextInt(200000) + 10000); // m²
                        a.put("productAmount", round1(1000 + rnd.nextDouble() * 15000)); // kg/yil
                        a.put("expectedRevenue", round1(30_000_000 + rnd.nextDouble() * 300_000_000));
                        a.put("netProfit", round1(6_000_000 + rnd.nextDouble() * 100_000_000));
                        a.put("tenant", rnd.nextBoolean() ? "MChJ Oqtosh Fish" : "MChJ Suv Hayoti");
                        a.put("govDecision", "№" + (200 + rnd.nextInt(800)) + "/2024");
                    }
                    default -> {}
                }
                f.setAttributes(a);

                f.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(45)));
                f.setUpdatedAt(LocalDateTime.now().minusDays(rnd.nextInt(15)));

                facilities.save(f);
                created++;
            }
        }
        System.out.println("FacilityDevSeeder: " + created + " ta demo obyekt yaratildi.");
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
