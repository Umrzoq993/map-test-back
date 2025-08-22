package com.agri.mapapp.stats.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MonthlyCount {
    private int month;   // 1..12
    private long count;
}
