package com.agri.mapapp.stats.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrgAgg {
    private Long orgId;
    private String orgName;
    private long count;
}
