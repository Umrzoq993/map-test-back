package com.agri.mapapp.facility.dto;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FacilityRes {
    private Long id;
    private Long orgId;
    private String orgName;
    private String name;
    private FacilityType type;      // ✅ Enum sifatida
    private FacilityStatus status;  // ✅ Enum sifatida
    private Double lat;
    private Double lng;
    private Integer zoom;
    private Object attributes;
    private Object geometry;
    private String createdAt;
    private String updatedAt;

    /** ✅ Entity → DTO converter */
    public static FacilityRes fromEntity(Facility f) {
        return FacilityRes.builder()
                .id(f.getId())
                .orgId(f.getOrg().getId())
                .orgName(f.getOrg().getName())
                .name(f.getName())
                .type(f.getType())       // Endi String emas, Enum bo‘ladi
                .status(f.getStatus())   // Endi String emas, Enum bo‘ladi
                .lat(f.getLat())
                .lng(f.getLng())
                .zoom(f.getZoom())
                .attributes(f.getAttributes())
                .geometry(f.getGeometry())
                .createdAt(f.getCreatedAt() != null ? f.getCreatedAt().toString() : null)
                .updatedAt(f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null)
                .build();
    }
}
