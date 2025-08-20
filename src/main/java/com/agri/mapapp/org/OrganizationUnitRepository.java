package com.agri.mapapp.org;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long>, JpaSpecificationExecutor<OrganizationUnit> {

    List<OrganizationUnit> findAllByOrderBySortOrderAscNameAsc();

    // YANGI: parent bo‘yicha tartiblangan siblings ro‘yxati
    List<OrganizationUnit> findByParentOrderBySortOrderAscNameAsc(OrganizationUnit parent);

    // YANGI: bolalari bor-yo‘qligini tekshirish
    boolean existsByParent(OrganizationUnit parent);
}
