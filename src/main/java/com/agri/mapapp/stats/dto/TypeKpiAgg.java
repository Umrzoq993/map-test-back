package com.agri.mapapp.stats.dto;

import com.agri.mapapp.facility.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class TypeKpiAgg {
    private FacilityType type;
    private Long count;
    private Double revenue;
    private Double profit;
    private Double current;
    private Double capacity;
    private Double utilPct; // current/capacity*100
}
