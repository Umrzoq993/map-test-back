package com.agri.mapapp.facility;

import com.agri.mapapp.facility.dto.*;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository repo;
    private final OrganizationUnitRepository orgRepo;

    @Transactional(readOnly = true)
    public FacilityRes get(Long id) {
        Facility f = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Facility not found: " + id));
        return toRes(f);
    }

    @Transactional
    public FacilityRes create(FacilityCreateReq req) {
        OrganizationUnit org = orgRepo.findById(req.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Org not found: " + req.getOrgId()));

        Facility f = Facility.builder()
                .org(org)
                .name(req.getName())
                .type(req.getType())
                .status(req.getStatus() == null ? FacilityStatus.ACTIVE : req.getStatus())
                .lat(req.getLat()).lng(req.getLng()).zoom(req.getZoom())
                .attributes(req.getAttributes())
                .geometry(req.getGeometry())
                .build();

        f = repo.save(f);
        return toRes(f);
    }

    @Transactional
    public FacilityRes put(Long id, FacilityPutReq req) {
        Facility f = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Facility not found: " + id));
        OrganizationUnit org = orgRepo.findById(req.getOrgId())
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

        return toRes(f);
    }

    @Transactional
    public FacilityRes patch(Long id, FacilityPatchReq req) {
        Facility f = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Facility not found: " + id));

        if (req.getOrgId() != null) {
            OrganizationUnit org = orgRepo.findById(req.getOrgId())
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

        return toRes(f);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("Facility not found: " + id);
        repo.deleteById(id);
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
                .geometry(f.getGeometry())
                .build();
    }

    /** JsonNode deep merge: updates dagi obyekt maydonlari target ga qo‘shiladi/yangilanadi */
    private JsonNode deepMerge(JsonNode target, JsonNode updates) {
        if (target == null || target.isNull()) return updates;
        if (updates == null || updates.isNull()) return target;

        if (target.isObject() && updates.isObject()) {
            ObjectNode t = (ObjectNode) target;
            updates.fieldNames().forEachRemaining((String field) -> {
                JsonNode updatedVal = updates.get(field);
                JsonNode existingVal = t.get(field);
                if (existingVal != null && existingVal.isObject() && updatedVal.isObject()) {
                    t.set(field, deepMerge(existingVal, updatedVal));
                } else {
                    t.set(field, updatedVal);
                }
            });
            return t;
        }
        // agar obyekt bo‘lmasa, yangisini qaytaramiz
        return updates;
    }
}
