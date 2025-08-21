package com.agri.mapapp.facility.validation;

import com.agri.mapapp.facility.FacilityType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * attributes/details JSON ichidagi maydonlarni type-ga qarab tekshiradi.
 * - CREATE/PUT da: mavjud bo'lsa tipini tekshiradi (majburiy emas).
 * - PATCH da: merge qilgandan so'ng xuddi shunday tekshiruvdan o'tadi.
 *
 * Eslatma: ma'lumotlar ixtiyoriy, shu sabab "required" qat'iy qo'yilmadi.
 * Agar majburiy qilishni istasangiz, pastdagi "req(...)" larni ishga solish mumkin.
 */
@Component
public class FacilityAttributesValidator {

    public void validate(FacilityType type, JsonNode attrs) {
        if (attrs == null || attrs.isNull()) return;

        List<String> errors = switch (type) {
            case GREENHOUSE -> validateGreenhouse(attrs);
            case POULTRY, COWSHED, TURKEY, SHEEPFOLD -> validateLivestock(attrs);
            case WORKSHOP -> validateWorkshop(attrs);
            case AUX_LAND, BORDER_LAND -> validateAgriLand(attrs);
            case FISHPOND -> validateFish(attrs);
            default -> new ArrayList<>();
        };

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes for " + type + ": " + String.join("; ", errors));
        }
    }

    // ---------- Schemas ----------

    private List<String> validateGreenhouse(JsonNode a) {
        List<String> e = new ArrayList<>();
        num(a, "totalAreaHa", e);
        str(a, "heatingType", e);
        num(a, "expectedYield", e);
        num(a, "expectedRevenue", e);
        num(a, "netProfit", e);
        return e;
    }

    private List<String> validateLivestock(JsonNode a) {
        List<String> e = new ArrayList<>();
        num(a, "areaM2", e);
        integer(a, "capacity", e);
        integer(a, "current", e);
        num(a, "productAmount", e);
        num(a, "expectedRevenue", e);
        num(a, "netProfit", e);
        // (ixtiyoriy) birlik kiritish: "productUnit": "kg" | "pcs"
        strOpt(a, "productUnit", e);
        return e;
    }

    private List<String> validateWorkshop(JsonNode a) {
        List<String> e = new ArrayList<>();
        num(a, "areaM2", e);
        num(a, "productAmount", e);
        num(a, "expectedRevenue", e);
        num(a, "netProfit", e);
        return e;
    }

    private List<String> validateAgriLand(JsonNode a) {
        List<String> e = new ArrayList<>();
        num(a, "areaM2", e);
        num(a, "expectedYield", e);
        num(a, "expectedRevenue", e);
        num(a, "netProfit", e);
        str(a, "tenant", e);
        str(a, "govDecision", e);
        return e;
    }

    private List<String> validateFish(JsonNode a) {
        List<String> e = new ArrayList<>();
        num(a, "areaM2", e);
        num(a, "productAmount", e);
        num(a, "expectedRevenue", e);
        num(a, "netProfit", e);
        str(a, "tenant", e);
        str(a, "govDecision", e);
        return e;
    }

    // ---------- Helpers (type checks) ----------

    private void num(JsonNode a, String field, List<String> e) {
        check(a, field, n -> n.isNumber(), "number", e);
    }

    private void integer(JsonNode a, String field, List<String> e) {
        check(a, field, n -> n.isInt() || n.isLong() || (n.isNumber() && n.asDouble() == Math.floor(n.asDouble())), "integer", e);
    }

    private void str(JsonNode a, String field, List<String> e) {
        check(a, field, JsonNode::isTextual, "string", e);
    }

    private void strOpt(JsonNode a, String field, List<String> e) {
        if (!a.has(field) || a.get(field).isNull()) return;
        str(a, field, e);
    }

    private void check(JsonNode a, String field, Function<JsonNode, Boolean> ok, String expected, List<String> e) {
        if (!a.has(field) || a.get(field).isNull()) return; // ixtiyoriy
        JsonNode v = a.get(field);
        if (!ok.apply(v)) {
            e.add("`" + field + "` must be " + expected);
        }
    }
}
