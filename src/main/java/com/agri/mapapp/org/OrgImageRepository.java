package com.agri.mapapp.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgImageRepository extends JpaRepository<OrgImage, Long> {
    List<OrgImage> findByOrg_IdOrderByCreatedAtDesc(Long orgId);
    long countByOrg_Id(Long orgId);
}

