package com.agri.mapapp.org;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationUnitRepository repo;

    public List<OrgNodeDto> getOrgTree() {
        List<OrganizationUnit> all = repo.findAllByOrderBySortOrderAscNameAsc();
        Map<Long, OrgNodeDto> map = new HashMap<>();
        List<OrgNodeDto> roots = new ArrayList<>();

        // Build nodes
        for (OrganizationUnit u : all) {
            OrgNodeDto dto = OrgNodeDto.builder()
                    .key(String.valueOf(u.getId()))
                    .title(u.getName())
                    .zoom(u.getZoom())
                    .pos(u.getLat() != null && u.getLng() != null ? new double[]{u.getLat(), u.getLng()} : null)
                    .build();
            map.put(u.getId(), dto);
        }

        // Link children
        for (OrganizationUnit u : all) {
            OrgNodeDto dto = map.get(u.getId());
            if (u.getParent() != null) {
                OrgNodeDto parentDto = map.get(u.getParent().getId());
                if (parentDto != null) parentDto.getChildren().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }
}