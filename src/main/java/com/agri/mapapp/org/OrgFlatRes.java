package com.agri.mapapp.org;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgFlatRes {
    private Long id;
    private String code;      // ✅ code qo‘shildi
    private String name;
    private Long parentId;
    private String parentName;
    private Double lat;
    private Double lng;
    private Integer zoom;
    private Integer sortOrder; // sibling tartibi
    private Integer depth;     // ildizdan chuqurlik
    private boolean hasChildren;

    public static OrgFlatRes of(OrganizationUnit u, String parentName, int depth, boolean hasChildren) {
        return OrgFlatRes.builder()
                .id(u.getId())
                .code(u.getCode()) // ✅ code to‘ldirildi
                .name(u.getName())
                .parentId(u.getParent() != null ? u.getParent().getId() : null)
                .parentName(parentName)
                .lat(u.getLat())
                .lng(u.getLng())
                .zoom(u.getZoom())
                .sortOrder(u.getSortOrder())
                .depth(depth)
                .hasChildren(hasChildren)
                .build();
    }
}
