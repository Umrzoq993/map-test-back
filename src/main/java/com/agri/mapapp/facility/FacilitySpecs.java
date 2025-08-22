// src/main/java/com/agri/mapapp/facility/FacilitySpecs.java
package com.agri.mapapp.facility;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

public final class FacilitySpecs {
    private FacilitySpecs(){}

    public static Specification<Facility> orgIn(Set<Long> ids) {
        return (root, cq, cb) -> {
            if (ids == null) return null;          // null => cheklanmagan (ADMIN)
            if (ids.isEmpty()) return cb.disjunction();
            return root.get("org").get("id").in(ids);
        };
    }

    public static Specification<Facility> typeIn(Collection<FacilityType> types) {
        return (root, cq, cb) -> {
            if (types == null || types.isEmpty()) return null;
            return root.get("type").in(types);
        };
    }

    public static Specification<Facility> statusEq(FacilityStatus st) {
        return (root, cq, cb) -> st == null ? null : cb.equal(root.get("status"), st);
    }

    public static Specification<Facility> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, cq, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    // --- Qo‘shimcha qulay spec’lar ---
    public static Specification<Facility> orgEq(Long orgId) {
        return (root, cq, cb) -> orgId == null ? null : cb.equal(root.get("org").get("id"), orgId);
    }

    public static Specification<Facility> typeEq(FacilityType type) {
        return (root, cq, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Facility> nameLike(String q) {
        return (root, cq, cb) -> {
            if (q == null || q.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase().trim() + "%");
        };
    }

}
