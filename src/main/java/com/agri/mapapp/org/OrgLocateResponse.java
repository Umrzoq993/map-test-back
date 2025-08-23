package com.agri.mapapp.org;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrgLocateResponse {
    private OrgDto org;                 // topilgan org + level
    private Viewport viewport;          // map uchun bbox/center (hisoblab kelamiz)
    private List<FacilityDto> facilities;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrgDto {
        private Long id;
        private String code;
        private String name;
        private Long parentId;
        private Integer level; // 0=root, 1=filial, >=2=bo‘lim
        private Double lat;
        private Double lng;
        private Integer zoom;

        public static OrgDto from(OrganizationUnit u, int level) {
            return OrgDto.builder()
                    .id(u.getId())
                    .code(u.getCode())
                    .name(u.getName())
                    .parentId(u.getParent()!=null? u.getParent().getId(): null)
                    .level(level)
                    .lat(u.getLat())
                    .lng(u.getLng())
                    .zoom(u.getZoom())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FacilityDto {
        private Long id;
        private String name;
        private FacilityType type;
        private FacilityStatus status;
        private Long orgId;
        private Double lat;
        private Double lng;

        public static FacilityDto from(Facility f) {
            return FacilityDto.builder()
                    .id(f.getId())
                    .name(f.getName())
                    .type(f.getType())
                    .status(f.getStatus())
                    .orgId(f.getOrg().getId())
                    .lat(f.getLat())
                    .lng(f.getLng())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Viewport {
        // Agar mumkin bo‘lsa fitBounds uchun:
        private Double minLat, minLng, maxLat, maxLng;
        // Fallback sifatida center/zoom:
        private Double centerLat, centerLng;
        private Integer zoom;
    }
}
