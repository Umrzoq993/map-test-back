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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orgs")
public class OrganizationController {
    private final OrganizationService service;
    private final AccessService access;
    // Qo‘shimcha: map qidiruv uchun (kod -> org + facilities + viewport)
    private final OrganizationLocateService locateService;

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

    // --- YANGI: Org’ni code bo‘yicha topish (yengil DTO) ---
    @GetMapping("/by-code/{code}")
    public ResponseEntity<OrgDto> getByCode(@PathVariable String code) {
        var org = service.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(OrgDto.from(org));
    }

    // --- YANGI: Map uchun locate (org + avlodlari resurslari + viewport) ---
    @GetMapping("/locate")
    public ResponseEntity<OrgLocateResponse> locate(Authentication auth, @RequestParam("code") String code) {
        var up = (UserPrincipal) auth.getPrincipal();
        Set<Long> allowed = access.allowedOrgIds(auth);
        Set<Long> scope = (up.getRole()== Role.ADMIN) ? null : allowed;
        var resp = locateService.locateByCode(code.trim(), scope);
        return ResponseEntity.ok(resp);
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

    // --- DTOs ---
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

    // YENGIL javob (by-code uchun). Frontendga kerak bo‘lgan minimal maydonlar:
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrgDto {
        private Long id;
        private String code;
        private String name;
        private Long parentId;
        private Double lat;
        private Double lng;
        private Integer zoom;

        public static OrgDto from(OrganizationUnit u) {
            return OrgDto.builder()
                    .id(u.getId())
                    .code(u.getCode())
                    .name(u.getName())
                    .parentId(u.getParent()!=null? u.getParent().getId(): null)
                    .lat(u.getLat())
                    .lng(u.getLng())
                    .zoom(u.getZoom())
                    .build();
        }
    }
}
