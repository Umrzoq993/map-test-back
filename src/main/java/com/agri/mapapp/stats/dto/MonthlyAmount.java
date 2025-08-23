package com.agri.mapapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class MonthlyAmount {
    private Integer month;   // 1..12
    private Double amount;   // masalan: revenue yoki profit summasi
}
