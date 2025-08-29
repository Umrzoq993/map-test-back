// src/main/java/com/agri/mapapp/org/OrgDetailsResponse.java
package com.agri.mapapp.org;

import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgDetailsResponse {

    /** Tanlangan org (level/depth bilan) */
    private OrgLocateResponse.OrgDto org;

    /** Root -> ... -> Parent -> Current tartibida breadcrumb */
    @Builder.Default
    private List<BreadcrumbItem> breadcrumb = List.of();

    /** Joriy orgning bevosita bolalari */
    @Builder.Default
    private List<NodeDto> children = List.of();

    /** Joriy orgning tengdosh (siblings)lari */
    @Builder.Default
    private List<NodeDto> siblings = List.of();

    /** Xarita uchun viewport (fitBounds yoki fallback center/zoom) */
    private OrgLocateResponse.Viewport viewport;

    /** Statistika (markerlar uchun ko‘rinadigan qisqa ma’lumotlar) */
    private FacilityStats stats;

    /** Shu org va uning allowed avlodlariga tegishli obyektlar ro‘yxati */
    @Builder.Default
    private List<OrgLocateResponse.FacilityDto> facilities = List.of();

    // --- Nested DTOs ---

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BreadcrumbItem {
        private Long id;
        private String code;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NodeDto {
        private Long id;
        private String code;
        private String name;
        private boolean hasChildren;
        private Double lat;
        private Double lng;
        private Integer zoom;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FacilityStats {
        private long total;
        private Map<FacilityType, Long> byType;
        private Map<FacilityStatus, Long> byStatus;
    }
}
