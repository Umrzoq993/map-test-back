package com.agri.mapapp.facility;

import com.agri.mapapp.facility.dto.*;
import com.agri.mapapp.org.OrganizationUnitRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityRepository repo;
    private final OrganizationUnitRepository orgRepo; // hozircha kerak bo'lmasa ham qoldirdik
    private final FacilityService service;

    // ===== LIST (multi-type + bbox) =====
    @GetMapping
    public List<FacilityRes> list(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) FacilityType type,
            @RequestParam(required = false, name = "types") String typesCsv,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) String q
    ) {
        // bbox parse: "minLng,minLat,maxLng,maxLat"
        Double minLat = null, maxLat = null, minLng = null, maxLng = null;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            if (parts.length == 4) {
                minLng = Double.valueOf(parts[0]);
                minLat = Double.valueOf(parts[1]);
                maxLng = Double.valueOf(parts[2]);
                maxLat = Double.valueOf(parts[3]);
            }
        }

        // types parse (CSV yoki legacy "type")
        List<FacilityType> types = null;
        if (typesCsv != null && !typesCsv.isBlank()) {
            types = Arrays.stream(typesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .map(FacilityType::valueOf)
                    .collect(Collectors.toList());
        } else if (type != null) {
            types = List.of(type);
        }

        // q ni oldindan lower + patternga aylantiramiz
        String qPattern = (q == null || q.isBlank())
                ? null
                : "%" + q.toLowerCase(Locale.ROOT) + "%";

        List<Facility> list;
        if (types != null && !types.isEmpty()) {
            list = repo.searchWithTypes(orgId, types, status, minLat, maxLat, minLng, maxLng, qPattern);
        } else {
            list = repo.searchNoType(orgId, status, minLat, maxLat, minLng, maxLng, qPattern);
        }

        return list.stream().map(this::toRes).toList();
    }

    // ===== READ BY ID =====
    @GetMapping("/{id}")
    public FacilityRes getById(@PathVariable Long id) {
        return service.get(id);
    }

    // ===== CREATE =====
    @PostMapping
    public ResponseEntity<FacilityRes> create(@Valid @RequestBody FacilityCreateReq req) {
        return ResponseEntity.ok(service.create(req));
    }

    // ===== PUT (REPLACE) =====
    @PutMapping("/{id}")
    public FacilityRes put(@PathVariable Long id, @Valid @RequestBody FacilityPutReq req) {
        return service.put(id, req);
    }

    // ===== PATCH (PARTIAL UPDATE) =====
    @PatchMapping("/{id}")
    public FacilityRes patch(@PathVariable Long id, @RequestBody FacilityPatchReq req) {
        return service.patch(id, req);
    }

    // ===== DELETE =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private FacilityRes toRes(Facility f) {
        return FacilityRes.builder()
                .id(f.getId())
                .orgId(f.getOrg().getId())
                .name(f.getName())
                .type(f.getType())
                .status(f.getStatus())
                .lat(f.getLat())
                .lng(f.getLng())
                .zoom(f.getZoom())
                .attributes(f.getAttributes())
                .build();
    }
}
