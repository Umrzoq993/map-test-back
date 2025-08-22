// src/main/java/com/agri/mapapp/org/OrganizationSpecs.java
package com.agri.mapapp.org;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Collection;

public final class OrganizationSpecs {

    private OrganizationSpecs() {}

    public static Specification<OrganizationUnit> nameLike(String q) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(q)) return null;
            return cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase().trim() + "%");
        };
    }

    public static Specification<OrganizationUnit> parentIsNull() {
        return (root, cq, cb) -> cb.isNull(root.get("parent"));
    }

    public static Specification<OrganizationUnit> parentEq(Long parentId) {
        return (root, cq, cb) -> {
            if (parentId == null) return cb.isNull(root.get("parent"));
            return cb.equal(root.get("parent").get("id"), parentId);
        };
    }

    public static Specification<OrganizationUnit> parentIn(Collection<Long> parentIds) {
        return (root, cq, cb) -> {
            if (parentIds == null || parentIds.isEmpty()) return cb.disjunction(); // hech narsa
            return root.get("parent").get("id").in(parentIds);
        };
    }

    public static Specification<OrganizationUnit> idIn(Collection<Long> ids) {
        return (root, cq, cb) -> {
            if (ids == null) return null;          // null => cheklanmagan (ADMIN)
            if (ids.isEmpty()) return cb.disjunction(); // bo'sh set => natija yo'q
            return root.get("id").in(ids);
        };
    }
}
