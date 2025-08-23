// src/main/java/com/agri/mapapp/bootstrap/DevSeeder.java
package com.agri.mapapp.bootstrap;

import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("dev")
public class DevSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeeder.class);

    private final OrganizationUnitRepository repo;

    public DevSeeder(OrganizationUnitRepository repo) {
        this.repo = repo;
    }

    // --- KOD GENERATSIYASI ---
    private String makeRootCode() {
        return "100000";
    }

    /** idx1: 1..6 → 110000, 120000, ... */
    private String makeBranchCode(int idx1) {
        return String.format("1%d0000", idx1);
    }

    /** branchIdx1: 1..6, seq1: 1..999 → 111001, 111002, 121001, ... */
    private String makeDeptCode(int branchIdx1, int seq1) {
        return String.format("1%d%03d", branchIdx1, seq1);
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Jadval bo‘sh bo‘lmasa seed qilmaymiz
        if (repo.count() > 0) {
            log.info("DevSeeder: org_unit bo‘sh emas, seed o‘tkazildi.");
            return;
        }

        // 1) ROOT (bitta)
        OrganizationUnit root = OrganizationUnit.builder()
                .name("Republic HQ")
                .code(makeRootCode())           // <<— KOD qo‘shildi
                .lat(41.311081)                 // Tashkent
                .lng(69.240562)
                .zoom(6)
                .sortOrder(0)
                .build();
        root = repo.save(root);

        // 2) 6 ta FILIAL
        List<BranchDef> branches = List.of(
                new BranchDef("Toshkent filiali", 41.311081, 69.240562, 8),
                new BranchDef("Farg‘ona filiali", 40.389000, 71.780000, 8),
                new BranchDef("Samarqand filiali", 39.654000, 66.959700, 8),
                new BranchDef("Buxoro filiali", 39.767000, 64.423000, 8),
                new BranchDef("Namangan filiali", 41.005000, 71.643000, 8),
                new BranchDef("Qashqadaryo filiali (Qarshi)", 38.859000, 65.789000, 8)
        );

        // 3) Tashkiliy BO‘LIMLAR (resurs emas!)
        String[] deptPool = {
                "Boshqaruv",
                "Rejalashtirish",
                "Monitoring va audit",
                "Logistika",
                "Moliya",
                "Kadrlar (HR)",
                "IT va ma’lumotlar",
                "Huquqiy xizmat",
                "Hududiy operatsiyalar"
        };

        int branchBaseOrder = 10;
        int created = 1; // root hisobida

        for (int i = 0; i < branches.size(); i++) {
            BranchDef b = branches.get(i);
            int branchIdx1 = i + 1;                 // 1..6
            AtomicInteger deptSeq = new AtomicInteger(1); // har filial uchun 1 dan boshlanadi

            // Filial
            OrganizationUnit branch = OrganizationUnit.builder()
                    .name(b.name)
                    .parent(root)
                    .code(makeBranchCode(branchIdx1)) // <<— KOD
                    .lat(b.lat)
                    .lng(b.lng)
                    .zoom(b.zoom)
                    .sortOrder(branchBaseOrder + i)   // 10,11,12...
                    .build();
            branch = repo.save(branch);
            created++;

            // Har filialga 3..9 ta bo‘lim
            int depCount = 3 + (i % 7); // 3..9
            for (int j = 0; j < depCount; j++) {
                String depName = deptPool[j % deptPool.length];

                // Demo uchun koordinatalarni filial atrofida ozgina siljitamiz
                double dLat = b.lat + (j * 0.008);
                double dLng = b.lng + (j * 0.008);

                OrganizationUnit dept = OrganizationUnit.builder()
                        .name(depName)
                        .parent(branch)
                        .code(makeDeptCode(branchIdx1, deptSeq.getAndIncrement())) // <<— KOD
                        .lat(dLat)
                        .lng(dLng)
                        .zoom(11)
                        .sortOrder((j + 1) * 10) // 10,20,30...
                        .build();
                repo.save(dept);
                created++;
            }
        }

        log.info("DevSeeder: OK — jami {} ta org (1 root, 6 filial, har birida 3–9 bo‘lim) KOD bilan yaratildi.", created);
        log.info("Eslatma: issiqxona/baliq ko‘li/molxona kabi resurslar bo‘limlarga tegishli obyektlar sifatida alohida seed qilinadi.");
    }

    private record BranchDef(String name, double lat, double lng, int zoom) {}
}
