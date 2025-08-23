package com.agri.mapapp.stats.dto;

import com.agri.mapapp.facility.FacilityType;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OverviewRes {
    private Long total;
    private Long active;

    private List<TypeAgg> types;                // count per type (legacy)

    private List<MonthlyCount> monthly;         // current period year months
    private List<MonthlyCount> prevMonthly;     // previous year months
    private Long yearNewCount;
    private Long prevYearNewCount;
    private Double yoyNewPct;

    // Finance monthlies
    private List<MonthlyAmount> revenueMonthly;
    private List<MonthlyAmount> revenuePrevMonthly;
    private Double revenueYtd;
    private Double revenuePrevYtd;
    private Double yoyRevenuePct;

    private List<MonthlyAmount> profitMonthly;
    private List<MonthlyAmount> profitPrevMonthly;
    private Double profitYtd;
    private Double profitPrevYtd;
    private Double yoyProfitPct;

    // Capacity & Productivity
    private Double capacityUtilPct;     // sum(current)/sum(capacity)*100
    private Double productivityKgPerM2; // sum(product_kg)/sum(area_m2)

    // Enriched breakdowns
    private List<TypeKpiAgg> typeKpis;          // type-based KPIs (revenue/profit/capacity/current)
    private List<OrgRevenueAgg> topOrgRevenue;  // TOP-10 org by revenue

    // Filters context
    private List<FacilityType> typesFilter;
    private Integer year;
    private String range;       // year|quarter|custom
    private Integer quarter;    // 1..4
    private String from;        // ISO (if custom)
    private String to;          // ISO (if custom)

    private List<OrgAgg> orgs;  // legacy org count
}
