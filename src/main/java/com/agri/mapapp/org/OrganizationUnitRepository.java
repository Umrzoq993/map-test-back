package com.agri.mapapp.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {
    List<OrganizationUnit> findAllByOrderBySortOrderAscNameAsc();
}
