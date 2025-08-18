package com.agri.mapapp.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drawings")
public class DrawingController {

    private final DrawingRepository repo;
    private final ObjectMapper om;

    @PostMapping
    public ResponseEntity<DrawingRes> save(@RequestBody SaveDrawingReq req) {
        // null bo‘lsa bo‘sh object node qo‘yamiz
        JsonNode payload = (req.getGeojson() != null) ? req.getGeojson() : om.createObjectNode();

        Drawing d = repo.save(Drawing.builder()
                .name(req.getName() == null ? "default" : req.getName())
                .geojson(payload)           // ✅ JsonNode berilyapti
                .build());

        DrawingRes body = DrawingRes.builder()
                .id(d.getId())
                .name(d.getName())
                .geojson(d.getGeojson())    // ✅ qayta o‘qish shart emas
                .createdAt(d.getCreatedAt().toString())
                .build();

        return ResponseEntity.ok(body);
    }

    @GetMapping("/latest")
    public ResponseEntity<DrawingRes> latest() {
        var opt = repo.findTopByOrderByCreatedAtDesc();
        if (opt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        var d = opt.get();

        DrawingRes body = DrawingRes.builder()
                .id(d.getId())
                .name(d.getName())
                .geojson(d.getGeojson())    // ✅ allaqachon JsonNode
                .createdAt(d.getCreatedAt().toString())
                .build();

        return ResponseEntity.ok(body);
    }
}
