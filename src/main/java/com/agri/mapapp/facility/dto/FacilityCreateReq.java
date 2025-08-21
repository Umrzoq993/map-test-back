package com.agri.mapapp.facility.dto;

import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacilityCreateReq {
    private Long orgId;
    private String name;
    private FacilityType type;
    private FacilityStatus status;
    private Double lat;
    private Double lng;
    private Integer zoom;

    /** Frontend "details" yuboradi, backend esa "attributes" saqlaydi */
    @JsonAlias({"details", "attributes"})
    private JsonNode attributes;

    private JsonNode geometry;
}
