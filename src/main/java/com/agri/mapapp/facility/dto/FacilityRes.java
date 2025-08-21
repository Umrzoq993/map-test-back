package com.agri.mapapp.facility.dto;

import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityRes {
    private Long id;
    private Long orgId;
    private String name;
    private FacilityType type;
    private FacilityStatus status;
    private Double lat;
    private Double lng;
    private Integer zoom;

    /** Frontendga "details" sifatida qaytaramiz (ichkarida attributes) */
    @JsonProperty("details")
    private JsonNode attributes;

    private JsonNode geometry;
}
