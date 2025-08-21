package com.agri.mapapp.facility.dto;

import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor
public class FacilityPutReq {
    @NotNull
    private Long orgId;
    @NotBlank private String name;
    @NotNull private FacilityType type;
    @NotNull private FacilityStatus status;
    private Double lat;
    private Double lng;
    private Integer zoom;

    /** PUT ham alias bilan ishlaydi */
    @JsonAlias({"details", "attributes"})
    private JsonNode attributes;

    private JsonNode geometry;
}
