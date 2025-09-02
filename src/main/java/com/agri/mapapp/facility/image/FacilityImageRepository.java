package com.agri.mapapp.facility.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityImageRepository extends JpaRepository<FacilityImage, Long> {
    List<FacilityImage> findByFacility_IdOrderByCreatedAtDesc(Long facilityId);
    long countByFacility_Id(Long facilityId);
}

