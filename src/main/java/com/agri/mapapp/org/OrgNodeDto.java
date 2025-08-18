package com.agri.mapapp.org;


import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgNodeDto {
    private String key;         // String bo‘lsin (frontend String kutyapti)
    private String title;       // name
    private double[] pos;       // [lat, lng] yoki null
    private Integer zoom;
    @Builder.Default
    private List<OrgNodeDto> children = new ArrayList<>();
}
