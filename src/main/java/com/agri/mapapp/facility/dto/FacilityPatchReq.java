package com.agri.mapapp.facility.dto;

import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityPatchReq {
    private Long orgId;
    private String name;
    private FacilityType type;
    private FacilityStatus status;
    private Double lat;
    private Double lng;
    private Integer zoom;

    /** PATCH ham "details" yoki "attributes" qabul qila oladi */
    @JsonAlias({"details", "attributes"})
    private JsonNode attributes; // deep-merge

    private JsonNode geometry;
}
