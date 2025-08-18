package com.agri.mapapp.map;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
class SaveDrawingReq {
    private String name;
    private JsonNode geojson;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class DrawingRes {
    private Long id;
    private String name;
    private JsonNode geojson;
    private String createdAt;
}
