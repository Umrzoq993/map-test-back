package com.agri.mapapp.org;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seed(OrganizationUnitRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            // HQ
            OrganizationUnit hq = repo.save(OrganizationUnit.builder()
                    .name("Markaziy Boshqarma (HQ)")
                    .lat(41.3111).lng(69.2797).zoom(13)
                    .sortOrder(1).build());

            repo.save(OrganizationUnit.builder()
                    .name("IT bo'limi").parent(hq)
                    .lat(41.3275).lng(69.2817).zoom(14)
                    .sortOrder(1).build());

            repo.save(OrganizationUnit.builder()
                    .name("Operatsiyalar bo'limi").parent(hq)
                    .lat(41.3059).lng(69.2690).zoom(14)
                    .sortOrder(2).build());

            // Hududiy filiallar (root node sifatida)
            OrganizationUnit regions = repo.save(OrganizationUnit.builder()
                    .name("Hududiy Filiallar")
                    .sortOrder(2).build());

            OrganizationUnit t = repo.save(OrganizationUnit.builder()
                    .name("Toshkent filiali").parent(regions)
                    .lat(41.2995).lng(69.2401).zoom(13)
                    .sortOrder(1).build());

            repo.save(OrganizationUnit.builder()
                    .name("Chilonzor bo'limi").parent(t)
                    .lat(41.2856).lng(69.2033).zoom(14).build());

            repo.save(OrganizationUnit.builder()
                    .name("Yunusobod bo'limi").parent(t)
                    .lat(41.3634).lng(69.2862).zoom(14).build());

            repo.save(OrganizationUnit.builder()
                    .name("Samarqand filiali").parent(regions)
                    .lat(39.6542).lng(66.9597).zoom(13)
                    .sortOrder(2).build());

            repo.save(OrganizationUnit.builder()
                    .name("Buxoro filiali").parent(regions)
                    .lat(39.7747).lng(64.4286).zoom(13)
                    .sortOrder(3).build());

            repo.save(OrganizationUnit.builder()
                    .name("Farg‘ona filiali").parent(regions)
                    .lat(40.3890).lng(71.7843).zoom(13)
                    .sortOrder(4).build());
        };
    }

}
