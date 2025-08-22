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
}
