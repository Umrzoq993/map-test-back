package com.agri.mapapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ByTypeRes {
    private String type;
    private long count;
    private double expectedRevenue;
    private double netProfit;
}
