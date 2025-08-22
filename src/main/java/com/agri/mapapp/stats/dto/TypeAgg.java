package com.agri.mapapp.stats.dto;

import com.agri.mapapp.facility.FacilityType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TypeAgg {
    private FacilityType type;
    private long count;
}
