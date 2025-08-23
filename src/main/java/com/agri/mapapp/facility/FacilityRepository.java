// src/main/java/com/agri/mapapp/facility/FacilityRepository.java
package com.agri.mapapp.facility;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {

    List<Facility> findByOrg_Id(Long orgId);
    List<Facility> findByOrgIdIn(Collection<Long> orgIds);

    @Query("""
select f from Facility f
where (:orgId is null or f.org.id = :orgId)
  and (:status is null or f.status = :status)
  and (:minLat is null or f.lat between :minLat and :maxLat)
  and (:minLng is null or f.lng between :minLng and :maxLng)
  and (:q is null or lower(f.name) like :q)
""")
    List<Facility> searchNoType(@Param("orgId") Long orgId,
                                @Param("status") FacilityStatus status,
                                @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                @Param("minLng") Double minLng, @Param("maxLng") Double maxLng,
                                @Param("q") String q);

    @Query("""
select f from Facility f
where (:orgId is null or f.org.id = :orgId)
  and f.type in :types
  and (:status is null or f.status = :status)
  and (:minLat is null or f.lat between :minLat and :maxLat)
  and (:minLng is null or f.lng between :minLng and :maxLng)
  and (:q is null or lower(f.name) like :q)
""")
    List<Facility> searchWithTypes(@Param("orgId") Long orgId,
                                   @Param("types") List<FacilityType> types,
                                   @Param("status") FacilityStatus status,
                                   @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                   @Param("minLng") Double minLng, @Param("maxLng") Double maxLng,
                                   @Param("q") String q);
}
