package com.agri.mapapp.facility;

import com.agri.mapapp.common.PageResponse;
import com.agri.mapapp.facility.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService service;

    /** ✅ Pagination + filter bilan facilities list */
    @GetMapping
    public PageResponse<FacilityRes> list(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) FacilityType type,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            // @ParameterObject Pageable pageable // Swagger uchun xohlasangiz
    ) {
        return service.getFacilitiesPage(orgId, type, status, q, pageable);
    }

    @GetMapping("/{id}")
    public FacilityRes getById(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FacilityRes> create(@Valid @RequestBody FacilityCreateReq req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public FacilityRes put(@PathVariable Long id, @Valid @RequestBody FacilityPutReq req) {
        return service.put(id, req);
    }

    @PatchMapping("/{id}")
    public FacilityRes patch(@PathVariable Long id, @RequestBody FacilityPatchReq req) {
        return service.patch(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
