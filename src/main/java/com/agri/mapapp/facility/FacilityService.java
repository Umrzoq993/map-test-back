package com.agri.mapapp.facility;

import com.agri.mapapp.common.PageResponse;
import com.agri.mapapp.facility.dto.FacilityCreateReq;
import com.agri.mapapp.facility.dto.FacilityPatchReq;
import com.agri.mapapp.facility.dto.FacilityPutReq;
import com.agri.mapapp.facility.dto.FacilityRes;
import com.agri.mapapp.facility.validation.FacilityAttributesValidator;
import com.agri.mapapp.org.OrganizationUnitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository repo;
    private final OrganizationUnitRepository orgRepo;
    private final FacilityAttributesValidator validator;

    /* ==================== LIST (OLD) ==================== */

    // Eski varianti (scope’siz) — orqaga moslik uchun qoldirildi
    public PageResponse<FacilityRes> getFacilitiesPage(
            Long orgId,
            FacilityType type,
            FacilityStatus status,
            String q,
            Pageable pageable
    ) {
        Specification<Facility> spec = Specification
                .where(FacilitySpecs.orgEq(orgId))
                .and(FacilitySpecs.typeEq(type))
                .and(FacilitySpecs.statusEq(status))
                .and(FacilitySpecs.nameLike(q));

        Page<Facility> page = repo.findAll(spec, pageable);
        Page<FacilityRes> mapped = page.map(this::toRes);
        return PageResponse.of(mapped);
    }

    // Yangi varianti (scope bilan)
    public PageResponse<FacilityRes> getFacilitiesPage(
            Long orgId,
            FacilityType type,
            FacilityStatus status,
            String q,
            Pageable pageable,
            Set<Long> allowedOrgIds
    ) {
        Specification<Facility> spec = Specification
                .where(FacilitySpecs.orgEq(orgId))             // aniq orgId bo‘lsa
                .and(FacilitySpecs.orgIn(allowedOrgIds))       // foydalanuvchi scope’i
                .and(FacilitySpecs.typeEq(type))
                .and(FacilitySpecs.statusEq(status))
                .and(FacilitySpecs.nameLike(q));

        Page<Facility> page = repo.findAll(spec, pageable);
        Page<FacilityRes> mapped = page.map(this::toRes);
        return PageResponse.of(mapped);
    }

    /* -------- all (time window, types) -------- */

    // Eski varianti (scope’siz) — orqaga moslik uchun qoldirildi
    public PageResponse<FacilityRes> getAllFacilities(Long orgId,
                                                      Collection<FacilityType> types,
                                                      FacilityStatus status,
                                                      LocalDateTime from,
                                                      LocalDateTime to,
                                                      Pageable pageable) {

        Specification<Facility> spec = Specification.where(
                        FacilitySpecs.orgIn(orgId == null ? null : Set.of(orgId)))
                .and(FacilitySpecs.typeIn(types))
                .and(FacilitySpecs.statusEq(status))
                .and(FacilitySpecs.createdBetween(from, to));

        Page<Facility> page = repo.findAll(spec, pageable);
        Page<FacilityRes> mapped = page.map(this::toRes);
        return PageResponse.of(mapped);
    }

    // Yangi varianti (scope bilan)
    public PageResponse<FacilityRes> getAllFacilities(Long orgId,
                                                      Collection<FacilityType> types,
                                                      FacilityStatus status,
                                                      LocalDateTime from,
                                                      LocalDateTime to,
                                                      Pageable pageable,
                                                      Set<Long> allowedOrgIds) {

        Specification<Facility> spec = Specification.where(
                        FacilitySpecs.orgEq(orgId))              // aniq orgId bo‘lsa
                .and(FacilitySpecs.orgIn(allowedOrgIds))        // foydalanuvchi scope’i
                .and(FacilitySpecs.typeIn(types))
                .and(FacilitySpecs.statusEq(status))
                .and(FacilitySpecs.createdBetween(from, to));

        Page<Facility> page = repo.findAll(spec, pageable);
        Page<FacilityRes> mapped = page.map(this::toRes);
        return PageResponse.of(mapped);
    }

    /* ==================== CRUD ==================== */

    public FacilityRes get(Long id) {
        Facility f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found: " + id));
        return toRes(f);
    }

    public FacilityRes create(FacilityCreateReq req) {
        var org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Org not found: " + req.getOrgId()));

        Facility f = new Facility();
        f.setOrg(org);
        f.setName(req.getName());
        f.setType(req.getType());
        f.setStatus(req.getStatus());
        f.setLat(req.getLat());
        f.setLng(req.getLng());
        f.setZoom(req.getZoom());
        f.setAttributes(req.getAttributes());
        f.setGeometry(req.getGeometry());
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());

        validator.validate(f.getType(), f.getAttributes());
        repo.save(f);
        return toRes(f);
    }

    public FacilityRes put(Long id, FacilityPutReq req) {
        Facility f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found: " + id));
        var org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Org not found: " + req.getOrgId()));

        f.setOrg(org);
        f.setName(req.getName());
        f.setType(req.getType());
        f.setStatus(req.getStatus());
        f.setLat(req.getLat());
        f.setLng(req.getLng());
        f.setZoom(req.getZoom());
        f.setAttributes(req.getAttributes());
        f.setGeometry(req.getGeometry());
        f.setUpdatedAt(LocalDateTime.now());

        validator.validate(f.getType(), f.getAttributes());
        repo.save(f);
        return toRes(f);
    }

    public FacilityRes patch(Long id, FacilityPatchReq req) {
        Facility f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found: " + id));

        if (req.getOrgId() != null) {
            var org = orgRepo.findById(req.getOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Org not found: " + req.getOrgId()));
            f.setOrg(org);
        }
        if (req.getName() != null) f.setName(req.getName());
        if (req.getType() != null) f.setType(req.getType());
        if (req.getStatus() != null) f.setStatus(req.getStatus());
        if (req.getLat() != null) f.setLat(req.getLat());
        if (req.getLng() != null) f.setLng(req.getLng());
        if (req.getZoom() != null) f.setZoom(req.getZoom());
        if (req.getGeometry() != null) f.setGeometry(req.getGeometry());
        if (req.getAttributes() != null) {
            JsonNode merged = deepMerge(f.getAttributes(), req.getAttributes());
            f.setAttributes(merged);
        }
        f.setUpdatedAt(LocalDateTime.now());

        validator.validate(f.getType(), f.getAttributes());
        repo.save(f);
        return toRes(f);
    }

    public void delete(Long id) {
        Facility f = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found: " + id));
        repo.delete(f);
    }

    /* ==================== Helpers ==================== */

    private FacilityRes toRes(Facility f) {
        return FacilityRes.builder()
                .id(f.getId())
                .orgId(f.getOrg() != null ? f.getOrg().getId() : null)
                .orgName(f.getOrg() != null ? f.getOrg().getName() : null)
                .name(f.getName())
                .type(f.getType())
                .status(f.getStatus())
                .lat(f.getLat())
                .lng(f.getLng())
                .zoom(f.getZoom())
                .attributes(f.getAttributes())
                .geometry(f.getGeometry())
                .createdAt(f.getCreatedAt() != null ? f.getCreatedAt().toString() : null)
                .updatedAt(f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null)
                .build();
    }

    private JsonNode deepMerge(JsonNode target, JsonNode updates) {
        // target yo‘q bo‘lsa - to‘g‘ridan-to‘g‘ri updates
        if (target == null) return updates;
        // Ikkalasi ham object bo‘lsa, maydonlar bo‘yicha mergelaymiz
        if (target.isObject() && updates != null && updates.isObject()) {
            // deepCopy() natijasini ObjectNode sifatida olaylik
            ObjectNode t = ((ObjectNode) target).deepCopy();

            updates.fieldNames().forEachRemaining(field -> {
                JsonNode updatedVal = updates.get(field);
                JsonNode existingVal = t.get(field);

                if (existingVal != null && existingVal.isObject() && updatedVal.isObject()) {
                    // Ikkalasi ham object => rekursiv merge
                    t.set(field, deepMerge(existingVal, updatedVal));
                } else {
                    // Aks holda, yangisini yozib qo‘yamiz
                    t.set(field, updatedVal);
                }
            });
            return t;
        }
        // Object emas bo‘lsa, updates bilan almashtiramiz (array yoki primitive hollarda)
        return updates;
    }
}
