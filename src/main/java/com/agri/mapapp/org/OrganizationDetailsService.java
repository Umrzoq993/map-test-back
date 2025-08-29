// src/main/java/com/agri/mapapp/org/OrganizationDetailsService.java
package com.agri.mapapp.org;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationDetailsService {

    private final OrganizationUnitRepository orgRepo;
    private final FacilityRepository facilityRepo;

    public OrgDetailsResponse getDetails(Long id, Set<Long> allowed) {
        OrganizationUnit org = orgRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Org not found: " + id));

        // Scope tekshiruvi: non-admin foydalanuvchi begona orgni ko‘ra olmaydi
        if (allowed != null && !allowed.contains(org.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Org not accessible: " + id);
        }

        // --- Breadcrumb (root -> ... -> current) ---
        List<OrgDetailsResponse.BreadcrumbItem> breadcrumb = buildBreadcrumb(org);

        // --- Children (bevosita) + hasChildren bayrogi ---
        List<OrganizationUnit> children = orgRepo.findByParentOrderBySortOrderAscNameAsc(org);
        if (allowed != null) {
            children.removeIf(c -> !allowed.contains(c.getId()));
        }
        List<OrgDetailsResponse.NodeDto> childDtos = children.stream()
                .map(c -> OrgDetailsResponse.NodeDto.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .name(c.getName())
                        .hasChildren(orgRepo.existsByParent(c))
                        .lat(c.getLat())
                        .lng(c.getLng())
                        .zoom(c.getZoom())
                        .build())
                .toList();

        // --- Siblings (parent ostidagi tengdoshlar) ---
        List<OrganizationUnit> siblings = orgRepo.findByParentOrderBySortOrderAscNameAsc(org.getParent());
        if (allowed != null) {
            siblings.removeIf(s -> !allowed.contains(s.getId()));
        }
        List<OrgDetailsResponse.NodeDto> siblingDtos = siblings.stream()
                .map(s -> OrgDetailsResponse.NodeDto.builder()
                        .id(s.getId())
                        .code(s.getCode())
                        .name(s.getName())
                        .hasChildren(orgRepo.existsByParent(s))
                        .lat(s.getLat())
                        .lng(s.getLng())
                        .zoom(s.getZoom())
                        .build())
                .toList();

        // --- Avlodlar IDs (shu orgni ham qo‘shamiz) ---
        List<Long> orgIds = new ArrayList<>();
        collectDescendantIds(org, orgIds);
        if (allowed != null) {
            orgIds.removeIf(x -> !allowed.contains(x));
        }

        // --- Facilities (shu org va allowed avlodlariga tegishli) ---
        List<Facility> facilities = orgIds.isEmpty()
                ? List.of()
                : facilityRepo.findByOrgIdIn(orgIds);

        // --- Statistika ---
        var byType = facilities.stream().collect(Collectors.groupingBy(Facility::getType, Collectors.counting()));
        var byStatus = facilities.stream().collect(Collectors.groupingBy(Facility::getStatus, Collectors.counting()));

        OrgDetailsResponse.FacilityStats stats = OrgDetailsResponse.FacilityStats.builder()
                .total(facilities.size())
                .byType(byType)
                .byStatus(byStatus)
                .build();

        // --- Viewport (org + avlod orglar + facilities koordinatalari asosida) ---
        OrgLocateResponse.Viewport viewport = buildViewport(org, orgIds, facilities);

        // --- Org DTO (level/depth bilan) ---
        int level = getDepth(org);
        OrgLocateResponse.OrgDto orgDto = OrgLocateResponse.OrgDto.from(org, level);

        // --- Facilities DTO ---
        var facilityDtos = facilities.stream()
                .map(OrgLocateResponse.FacilityDto::from)
                .toList();

        return OrgDetailsResponse.builder()
                .org(orgDto)
                .breadcrumb(breadcrumb)
                .children(childDtos)
                .siblings(siblingDtos)
                .viewport(viewport)
                .stats(stats)
                .facilities(facilityDtos)
                .build();
    }

    private List<OrgDetailsResponse.BreadcrumbItem> buildBreadcrumb(OrganizationUnit leaf) {
        List<OrgDetailsResponse.BreadcrumbItem> list = new ArrayList<>();
        OrganizationUnit cur = leaf;
        int guard = 0;
        while (cur != null && guard++ < 10000) {
            list.add(OrgDetailsResponse.BreadcrumbItem.builder()
                    .id(cur.getId())
                    .code(cur.getCode())
                    .name(cur.getName())
                    .build());
            cur = cur.getParent();
        }
        Collections.reverse(list);
        return list;
    }

    private void collectDescendantIds(OrganizationUnit root, List<Long> acc) {
        Deque<OrganizationUnit> dq = new ArrayDeque<>();
        dq.add(root);
        int guard = 0;
        while (!dq.isEmpty() && guard++ < 1_000_000) {
            OrganizationUnit cur = dq.pop();
            acc.add(cur.getId());
            List<OrganizationUnit> children = orgRepo.findByParentOrderBySortOrderAscNameAsc(cur);
            dq.addAll(children);
        }
    }

    private int getDepth(OrganizationUnit node) {
        int d = 0;
        OrganizationUnit p = node.getParent();
        while (p != null) { d++; p = p.getParent(); }
        return d;
    }

    private OrgLocateResponse.Viewport buildViewport(OrganizationUnit org,
                                                     List<Long> orgIds,
                                                     List<Facility> facilities) {
        List<double[]> points = new ArrayList<>();

        // Orglarning koordinatalari
        if (!orgIds.isEmpty()) {
            for (OrganizationUnit u : orgRepo.findAllById(orgIds)) {
                if (u.getLat() != null && u.getLng() != null) {
                    points.add(new double[]{u.getLat(), u.getLng()});
                }
            }
        } else {
            // Faqat o‘zi
            if (org.getLat() != null && org.getLng() != null) {
                points.add(new double[]{org.getLat(), org.getLng()});
            }
        }

        // Facilities koordinatalari
        for (Facility f : facilities) {
            if (f.getLat() != null && f.getLng() != null) {
                points.add(new double[]{f.getLat(), f.getLng()});
            }
        }

        OrgLocateResponse.Viewport.ViewportBuilder b = OrgLocateResponse.Viewport.builder();

        if (!points.isEmpty()) {
            double minLat = Double.POSITIVE_INFINITY, minLng = Double.POSITIVE_INFINITY;
            double maxLat = Double.NEGATIVE_INFINITY, maxLng = Double.NEGATIVE_INFINITY;
            double sumLat = 0, sumLng = 0;

            for (double[] p : points) {
                double lat = p[0], lng = p[1];
                sumLat += lat; sumLng += lng;
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lng < minLng) minLng = lng;
                if (lng > maxLng) maxLng = lng;
            }

            double centerLat = sumLat / points.size();
            double centerLng = sumLng / points.size();

            b.minLat(minLat).minLng(minLng).maxLat(maxLat).maxLng(maxLng)
                    .centerLat(centerLat).centerLng(centerLng);
        } else {
            // Fallback: markaz ma’lum bo‘lsa – o‘sha, bo‘lmasa null
            b.centerLat(org.getLat()).centerLng(org.getLng());
        }

        // Zoom: agar orgda zoom bo‘lsa, shuni; aks holda konservativ default
        Integer zoom = org.getZoom();
        if (zoom == null) {
            int level = getDepth(org);
            zoom = (level >= 2) ? 12 : (level == 1 ? 9 : 6);
        }
        b.zoom(zoom);

        return b.build();
    }
}
