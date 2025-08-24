package com.agri.mapapp.bootstrap;

import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("dev")
@Order(1) // ✅ avval org daraxtini seedyapmiz
public class DevSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevSeeder.class);

    private final OrganizationUnitRepository repo;

    public DevSeeder(OrganizationUnitRepository repo) {
        this.repo = repo;
    }

    // --- KOD GENERATSIYASI ---
    private String makeRootCode() { return "100000"; }
    private String makeBranchCode(int idx1) { return String.format("1%d0000", idx1); }
    private String makeDeptCode(int branchIdx1, int seq1) { return String.format("1%d%03d", branchIdx1, seq1); }

    @Override
    @Transactional
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("DevSeeder: org_unit bo‘sh emas, seed o‘tkazildi.");
            return;
        }

        OrganizationUnit root = OrganizationUnit.builder()
                .name("Republic HQ")
                .code(makeRootCode())
                .lat(41.311081)
                .lng(69.240562)
                .zoom(6)
                .sortOrder(0)
                .build();
        root = repo.save(root);

        List<BranchDef> branches = List.of(
                new BranchDef("Toshkent filiali", 41.311081, 69.240562, 8),
                new BranchDef("Farg‘ona filiali", 40.389000, 71.780000, 8),
                new BranchDef("Samarqand filiali", 39.654000, 66.959700, 8),
                new BranchDef("Buxoro filiali", 39.767000, 64.423000, 8),
                new BranchDef("Namangan filiali", 41.005000, 71.643000, 8),
                new BranchDef("Qashqadaryo filiali (Qarshi)", 38.859000, 65.789000, 8)
        );

        String[] deptPool = {
                "Boshqaruv","Rejalashtirish","Monitoring va audit","Logistika",
                "Moliya","Kadrlar (HR)","IT va ma’lumotlar","Huquqiy xizmat","Hududiy operatsiyalar"
        };

        int branchBaseOrder = 10;
        int created = 1; // root

        for (int i = 0; i < branches.size(); i++) {
            BranchDef b = branches.get(i);
            int branchIdx1 = i + 1;
            AtomicInteger deptSeq = new AtomicInteger(1);

            OrganizationUnit branch = OrganizationUnit.builder()
                    .name(b.name)
                    .parent(root)
                    .code(makeBranchCode(branchIdx1))
                    .lat(b.lat)
                    .lng(b.lng)
                    .zoom(b.zoom)
                    .sortOrder(branchBaseOrder + i)
                    .build();
            branch = repo.save(branch);
            created++;

            int depCount = 3 + (i % 7); // 3..9
            for (int j = 0; j < depCount; j++) {
                String depName = deptPool[j % deptPool.length];
                double dLat = b.lat + (j * 0.008);
                double dLng = b.lng + (j * 0.008);

                OrganizationUnit dept = OrganizationUnit.builder()
                        .name(depName)
                        .parent(branch)
                        .code(makeDeptCode(branchIdx1, deptSeq.getAndIncrement()))
                        .lat(dLat)
                        .lng(dLng)
                        .zoom(11)
                        .sortOrder((j + 1) * 10)
                        .build();
                repo.save(dept);
                created++;
            }
        }

        log.info("DevSeeder: OK — jami {} ta org (1 root, 6 filial, har birida 3–9 bo‘lim) KOD bilan yaratildi.", created);
    }

    private record BranchDef(String name, double lat, double lng, int zoom) {}
}
