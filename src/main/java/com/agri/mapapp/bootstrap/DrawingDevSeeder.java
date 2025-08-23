// src/main/java/com/agri/mapapp/bootstrap/DrawingDevSeeder.java
package com.agri.mapapp.bootstrap;

import com.agri.mapapp.map.Drawing;
import com.agri.mapapp.map.DrawingRepository;
import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class DrawingDevSeeder implements ApplicationRunner {

    private final OrganizationUnitRepository orgs;
    private final DrawingRepository drawings;
    private final ObjectMapper om;

    @Override
    public void run(ApplicationArguments args) {
        if (drawings.count() > 0) return;

        var all = orgs.findAllByOrderBySortOrderAscNameAsc();
        OrganizationUnit root = all.stream().filter(o -> o.getParent() == null).findFirst().orElse(null);
        if (root == null) return;

        // Filiallar (root bolalari)
        var branches = all.stream().filter(o -> o.getParent() != null && o.getParent().getId().equals(root.getId())).toList();

        int created = 0;
        for (int i = 0; i < branches.size(); i++) {
            OrganizationUnit b = branches.get(i);
            double lat = b.getLat() != null ? b.getLat() : 41.0 + i * 0.2;
            double lng = b.getLng() != null ? b.getLng() : 69.0 + i * 0.2;

            // 1) Nazorat hududi (kvadrat poligon)
            ObjectNode poly = featureCollectionPolygon(lat, lng, 0.08);
            created += save("[" + b.getName() + "] Nazorat hududi", poly);

            // 2) Marshrut (LineString)
            ObjectNode line = featureCollectionLine(lat, lng, 4);
            created += save("[" + b.getName() + "] Tekshiruv marshruti", line);

            // 3) Belgilar (Pointlar to‘plami)
            ObjectNode points = featureCollectionPoints(lat, lng, 5);
            created += save("[" + b.getName() + "] Belgilar", points);
        }

        // Respublika uchun umumiy kontur (katta poligon)
        if (!branches.isEmpty()) {
            double lat = branches.stream().map(OrganizationUnit::getLat).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(41.3);
            double lng = branches.stream().map(OrganizationUnit::getLng).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(69.2);
            ObjectNode big = featureCollectionPolygon(lat, lng, 0.6);
            created += save("[Respublika] Umumiy kontur", big);
        }

        System.out.println("DrawingDevSeeder: " + created + " ta drawing yaratildi.");
    }

    private int save(String name, ObjectNode geojson) {
        Drawing d = new Drawing();
        d.setName(name);
        d.setGeojson(geojson);
        d.setCreatedAt(LocalDateTime.now());
        drawings.save(d);
        return 1;
    }

    private ObjectNode featureCollectionPolygon(double lat, double lng, double delta) {
        // Kvadrat polygon
        ArrayNode ring = om.createArrayNode();
        ring.add(coord(lng - delta, lat - delta));
        ring.add(coord(lng + delta, lat - delta));
        ring.add(coord(lng + delta, lat + delta));
        ring.add(coord(lng - delta, lat + delta));
        ring.add(coord(lng - delta, lat - delta));

        ArrayNode rings = om.createArrayNode().add(ring);

        ObjectNode polygon = om.createObjectNode();
        polygon.put("type", "Polygon");
        polygon.set("coordinates", rings);

        ObjectNode feat = feature(polygon, "hudud", "nazorat");
        return featureCollection(feat);
    }

    private ObjectNode featureCollectionLine(double lat, double lng, int points) {
        ArrayNode coords = om.createArrayNode();
        for (int i = 0; i < points; i++) {
            double dx = (i * 0.03);
            coords.add(coord(lng - 0.05 + dx, lat + Math.sin(i) * 0.02));
        }
        ObjectNode line = om.createObjectNode();
        line.put("type", "LineString");
        line.set("coordinates", coords);

        ObjectNode feat = feature(line, "marshrut", "tekshiruv");
        return featureCollection(feat);
    }

    private ObjectNode featureCollectionPoints(double lat, double lng, int n) {
        ArrayNode feats = om.createArrayNode();
        for (int i = 0; i < n; i++) {
            ObjectNode pt = om.createObjectNode();
            pt.put("type", "Point");
            pt.set("coordinates", coord(lng + (i - n / 2.0) * 0.02, lat + Math.cos(i) * 0.015));

            feats.add(feature(pt, "belgi_" + (i + 1), i % 2 == 0 ? "ok" : "e'tibor"));
        }
        ObjectNode fc = om.createObjectNode();
        fc.put("type", "FeatureCollection");
        fc.set("features", feats);
        return fc;
    }

    private ObjectNode featureCollection(ObjectNode... features) {
        ObjectNode fc = om.createObjectNode();
        fc.put("type", "FeatureCollection");
        ArrayNode arr = om.createArrayNode();
        for (ObjectNode f : features) arr.add(f);
        fc.set("features", arr);
        return fc;
    }

    private ObjectNode feature(ObjectNode geometry, String name, String status) {
        ObjectNode f = om.createObjectNode();
        f.put("type", "Feature");
        f.set("geometry", geometry);
        ObjectNode prop = om.createObjectNode();
        prop.put("name", name);
        prop.put("status", status);
        f.set("properties", prop);
        return f;
    }

    private ArrayNode coord(double lng, double lat) {
        ArrayNode c = om.createArrayNode();
        c.add(lng);
        c.add(lat);
        return c;
    }
}
