package com.agri.mapapp.facility;

import com.agri.mapapp.common.PageResponse;
import com.agri.mapapp.facility.dto.FacilityCreateReq;
import com.agri.mapapp.facility.dto.FacilityPatchReq;
import com.agri.mapapp.facility.dto.FacilityPutReq;
import com.agri.mapapp.facility.dto.FacilityRes;
import com.agri.mapapp.org.AccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService service;
    private final AccessService accessService;

    /**
     * Paginatsiyadagi ro‘yxat.
     * Foydalanuvchi scope’i (ADMIN => hammasi, ORG_USER => subtree) majburan qo‘llanadi.
     */
    @GetMapping
    public PageResponse<FacilityRes> list(
            Authentication auth,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) FacilityType type,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Set<Long> allowed = accessService.allowedOrgIds(auth); // ADMIN -> null, ORG_USER -> subtree ids
        // Agar foydalanuvchi aniq orgId so‘rasa va bu orgId uning scope’iga kirmasa — rad etamiz
        if (orgId != null && allowed != null && !allowed.contains(orgId)) {
            throw new AccessDeniedException("Siz ushbu bo‘lim obyektlarini ko‘ra olmaysiz.");
        }
        return service.getFacilitiesPage(orgId, type, status, q, pageable, allowed);
    }

    /**
     * Vaqt oraliqli va ko‘p turli filtrlash (paginatsiya bilan).
     * Foydalanuvchi scope’i majburan qo‘llanadi.
     *
     * Query misollar:
     *   /api/facilities/all?types=GREENHOUSE,POULTRY_EGG&status=ACTIVE&from=2025-01-01T00:00:00&to=2025-08-01T00:00:00
     */
    @GetMapping("/all")
    public PageResponse<FacilityRes> listAll(
            Authentication auth,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) List<FacilityType> types,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Set<Long> allowed = accessService.allowedOrgIds(auth);
        if (orgId != null && allowed != null && !allowed.contains(orgId)) {
            throw new AccessDeniedException("Siz ushbu bo‘lim obyektlarini ko‘ra olmaysiz.");
        }
        return service.getAllFacilities(orgId, types, status, from, to, pageable, allowed);
    }

    /* ====== CRUD (o‘zgarmagan) ====== */

    @GetMapping("/{id}")
    public FacilityRes getById(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public FacilityRes create(@Valid @RequestBody FacilityCreateReq req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public FacilityRes put(@PathVariable Long id, @Valid @RequestBody FacilityPutReq req) {
        return service.put(id, req);
    }

    @PatchMapping("/{id}")
    public FacilityRes patch(@PathVariable Long id, @Valid @RequestBody FacilityPatchReq req) {
        return service.patch(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
