package com.agri.mapapp.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByUsername(String username);

    @Query("""
        select u from AppUser u
        where (:q is null or
               lower(u.username) like :q or
               lower(u.fullName) like :q or
               lower(u.position) like :q or
               lower(u.title) like :q or
               lower(u.phone) like :q)
          and (:role is null or u.role = :role)
          and (:status is null or u.status = :status)
          and (:orgId is null or (u.orgUnit is not null and u.orgUnit.id = :orgId))
          and (:dept is null or u.department = :dept)
    """)
    Page<AppUser> search(@Param("q") String qPattern,
                         @Param("role") Role role,
                         @Param("status") UserStatus status,
                         @Param("orgId") Long orgId,
                         @Param("dept") String department,
                         Pageable pageable);
}
