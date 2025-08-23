package com.agri.mapapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrgRevenueAgg {
    private Long orgId;
    private String orgName;
    private Long count;
    private Double revenue;
    private Double profit;
}
