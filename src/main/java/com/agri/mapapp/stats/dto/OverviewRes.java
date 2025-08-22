package com.agri.mapapp.stats.dto;

import com.agri.mapapp.facility.FacilityType;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OverviewRes {
    /** Umumiy inshootlar soni (filterlar qo‘llangandan keyin) */
    private Long total;

    /** ACTIVE holatidagi inshootlar soni */
    private Long active;

    /** Turlar kesimidagi agregatsiya */
    private List<TypeAgg> types;

    /** Oylar bo‘yicha yaratilganlar soni */
    private List<MonthlyCount> monthly;

    /** Bo‘limlar (org) bo‘yicha taqsimot */
    private List<OrgAgg> orgs;

    /** Kontekst: ko‘rilayotgan yil (ixtiyoriy) */
    private Integer year;

    /** Qo‘llangan type filtrlar (ixtiyoriy) */
    private List<FacilityType> typesFilter;
}
