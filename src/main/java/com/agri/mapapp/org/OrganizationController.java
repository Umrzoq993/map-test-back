package com.agri.mapapp.org;

import com.agri.mapapp.auth.Role;
import com.agri.mapapp.auth.UserPrincipal;
import com.agri.mapapp.common.PageResponse;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orgs")
public class OrganizationController {
    private final OrganizationService service;
    private final AccessService access;

    @GetMapping("/tree")
    public ResponseEntity<List<OrgNodeDto>> tree(Authentication auth) {
        Set<Long> allowed = access.allowedOrgIds(auth);
        return ResponseEntity.ok(service.getOrgTreeForUser(allowed));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrgFlatRes>> page(
            Authentication auth,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long parentId,
            @PageableDefault(size = 20, sort = {"sortOrder","name"}) Pageable pageable
    ) {
        Set<Long> allowed = access.allowedOrgIds(auth);
        if (parentId != null && allowed != null && !allowed.contains(parentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.findPageForUser(q, parentId, pageable, allowed));
    }

    // CREATE
    @PostMapping
    public ResponseEntity<OrgFlatRes> create(Authentication auth, @RequestBody CreateOrgReq req) {
        var up = (UserPrincipal) auth.getPrincipal();
        if (up.getRole() != Role.ADMIN) {
            Long pid = req.getParentId();
            // Root ostiga yaratishga ham faqat o‘z subtree ichida ruxsat (root uchun pid=null -> deny)
            if (pid == null || !access.canAccessOrg(auth, pid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        var u = service.createOrg(req.getName(), req.getParentId(), req.getLat(), req.getLng(), req.getZoom(), req.getSortOrder());
        var res = OrgFlatRes.of(u, u.getParent()!=null? u.getParent().getName(): null, 0, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<OrgFlatRes> update(Authentication auth, @PathVariable Long id, @RequestBody UpdateOrgReq req) {
        // Tahrirlashdan oldin shu tugun foydalanuvchi doirasida ekanligini tekshiramiz
        if (!access.canAccessOrg(auth, id) && ((UserPrincipal)auth.getPrincipal()).getRole()!=Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var u = service.updateOrg(id, req.getName(), req.getLat(), req.getLng(), req.getZoom(), req.getSortOrder());
        var res = OrgFlatRes.of(u, u.getParent()!=null? u.getParent().getName(): null, 0, false);
        return ResponseEntity.ok(res);
    }

    // MOVE
    @PatchMapping("/{id}/move")
    public ResponseEntity<OrgFlatRes> move(Authentication auth, @PathVariable Long id, @RequestBody MoveOrgReq req) {
        var up = (UserPrincipal) auth.getPrincipal();
        if (up.getRole() != Role.ADMIN) {
            if (!access.canAccessOrg(auth, id) || (req.getNewParentId()==null) || !access.canAccessOrg(auth, req.getNewParentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        var u = service.moveOrg(id, req.getNewParentId(), req.getOrderIndex());
        var res = OrgFlatRes.of(u, u.getParent()!=null? u.getParent().getName(): null, 0, false);
        return ResponseEntity.ok(res);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        if (!access.canAccessOrg(auth, id) && ((UserPrincipal)auth.getPrincipal()).getRole()!=Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.deleteOrg(id);
        return ResponseEntity.noContent().build();
    }

    // DTOs (oldingidek)
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateOrgReq {
        private String name; private Long parentId; private Double lat; private Double lng; private Integer zoom; private Integer sortOrder;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateOrgReq {
        private String name; private Double lat; private Double lng; private Integer zoom; private Integer sortOrder;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MoveOrgReq {
        private Long newParentId; private Integer orderIndex;
    }
}
