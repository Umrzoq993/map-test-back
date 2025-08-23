package com.agri.mapapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class KpiRes {
    private long totalFacilities;
    private long activeFacilities;
    private double totalExpectedRevenue; // yig'indi
    private double totalNetProfit;       // yig'indi
}
