package com.agri.mapapp.bootstrap;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

@Component
@Profile("dev")
@Order(5)
@RequiredArgsConstructor
public class FacilityGeometrySeeder implements ApplicationRunner {

    // ❗ Faqat mavjud enum qiymatlari: FIELD/ORCHARD/FISHFARM yo‘q
    private static final EnumSet<FacilityType> POLY_TYPES = EnumSet.of(
            FacilityType.AUX_LAND,
            FacilityType.BORDER_LAND,
            FacilityType.FISHPOND,
            FacilityType.GREENHOUSE
    );

    private final FacilityRepository facilities;
    private final ObjectMapper om;
    private final Random rnd = new Random(17);

    @Override
    public void run(ApplicationArguments args) {
        // Poligon geometry yo‘q bo‘lgan, ammo poligon bo‘lishi ma’qul turlardan 1/2 qismini to‘ldiramiz
        List<Facility> candidates = facilities.findAll().stream()
                .filter(f -> f.getType() != null && POLY_TYPES.contains(f.getType()))
                .filter(f -> f.getGeometry() == null)
                .toList();

        int updated = 0;
        for (Facility f : candidates) {
            // Taxminan yarmini to‘ldiramiz
            if ((updated % 2) == 1) {
                updated++;
                continue;
            }
            if (f.getLat() == null || f.getLng() == null) {
                updated++;
                continue;
            }

            double delta = 0.003 + rnd.nextDouble() * 0.004; // ~300–700m kvadrat
            ObjectNode poly = squarePolygon(f.getLat(), f.getLng(), delta);
            f.setGeometry(poly);
            facilities.save(f);
            updated++;
        }
        System.out.println("FacilityGeometrySeeder: " + updated + " ta obyektga polygon geometry berildi.");
    }

    private ObjectNode squarePolygon(double lat, double lng, double delta) {
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

        ObjectNode feature = om.createObjectNode();
        feature.put("type", "Feature");
        feature.set("geometry", polygon);
        feature.set("properties", om.createObjectNode().put("source", "seeder"));

        ObjectNode fc = om.createObjectNode();
        fc.put("type", "FeatureCollection");
        fc.set("features", om.createArrayNode().add(feature));
        return fc;
    }

    private ArrayNode coord(double lng, double lat) {
        ArrayNode c = om.createArrayNode();
        c.add(lng);
        c.add(lat);
        return c;
    }
}
