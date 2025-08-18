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
    // SeedData.java ichida, org'lar yaratilgandan keyin:
    @Bean CommandLineRunner seedFacilities(FacilityRepository fRepo, OrganizationUnitRepository orgRepo) {
        return args -> {
            if (fRepo.count() > 0) return;
            var hq = orgRepo.findAll().stream().findFirst().orElse(null);
            if (hq == null) return;

            fRepo.save(Facility.builder()
                    .org(hq).name("HQ Issiqxona 1")
                    .type(FacilityType.GREENHOUSE).status(FacilityStatus.ACTIVE)
                    .lat(41.312).lng(69.28).zoom(15)
                    .build());

            fRepo.save(Facility.builder()
                    .org(hq).name("HQ Molxona A")
                    .type(FacilityType.COWSHED).status(FacilityStatus.ACTIVE)
                    .lat(41.309).lng(69.275).zoom(15)
                    .build());
        };
    }

}
