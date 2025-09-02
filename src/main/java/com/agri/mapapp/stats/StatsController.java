package com.agri.mapapp.stats;

import com.agri.mapapp.auth.Role;
import com.agri.mapapp.auth.UserPrincipal;
import com.agri.mapapp.facility.FacilityType;
import com.agri.mapapp.org.AccessService;
import com.agri.mapapp.stats.dto.OverviewRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final AccessService accessService;

    @GetMapping("/overview")
    public OverviewRes overview(
            Authentication auth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "year") String range, // year|quarter|custom
            @RequestParam(required = false) Integer quarter,                      // 1..4 (range=quarter bo'lsa)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String types, // CSV of FacilityType
            @RequestParam(required = false) Long orgId    // optional single org filter
    ) {
        Set<Long> allowedOrgIds = resolveAllowedScope(auth);
        if (orgId != null) {
            // subtree of requested org
            Set<Long> requestedSubtree = accessService.subtreeOf(orgId);
            if (allowedOrgIds == null) { // ADMIN -> just use subtree (may be empty if org not found)
                allowedOrgIds = requestedSubtree;
            } else {
                if (allowedOrgIds.contains(orgId)) {
                    // narrow to intersection to be extra safe (though subtree should be subset)
                    requestedSubtree.retainAll(allowedOrgIds);
                    allowedOrgIds = requestedSubtree;
                } else {
                    allowedOrgIds = Collections.emptySet();
                }
            }
        }
        List<FacilityType> typeList = parseTypes(types);
        OverviewRes res = statsService.overviewAdvanced(allowedOrgIds, year, typeList, range, quarter, from, to);
        if (orgId != null) res.setOrgFilter(orgId);
        return res;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            Authentication auth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "year") String range,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) Long orgId
    ) {
        Set<Long> allowedOrgIds = resolveAllowedScope(auth);
        if (orgId != null) {
            Set<Long> requestedSubtree = accessService.subtreeOf(orgId);
            if (allowedOrgIds == null) {
                allowedOrgIds = requestedSubtree;
            } else {
                if (allowedOrgIds.contains(orgId)) {
                    requestedSubtree.retainAll(allowedOrgIds);
                    allowedOrgIds = requestedSubtree;
                } else {
                    allowedOrgIds = Collections.emptySet();
                }
            }
        }
        List<FacilityType> typeList = parseTypes(types);
        String csv = statsService.exportCsv(allowedOrgIds, year, typeList, range, quarter, from, to);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stats-export.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }

    // ================= Helpers =================

    /** ADMIN => null (hammasi), USER => o‘zi + barcha avlodlari id’lari to‘plami */
    private Set<Long> resolveAllowedScope(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            return Collections.emptySet(); // no auth => hech narsa
        }
        if (up.getRole() == Role.ADMIN) {
            return null; // ADMIN => to‘liq ko‘rish
        }
        return accessService.allowedOrgIds(auth); // USER => subtree
    }

    private List<FacilityType> parseTypes(String types) {
        if (types == null || types.isBlank()) return null;
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FacilityType::valueOf)
                .collect(Collectors.toList());
    }

    // (ixtiyoriy) UI’da ko‘lamni ko‘rsatish uchun kichik endpoint
    @GetMapping("/scope")
    public Map<String, Object> scope(Authentication auth) {
        Set<Long> allowed = resolveAllowedScope(auth);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("admin", allowed == null);
        m.put("allowedCount", allowed == null ? -1 : allowed.size());
        m.put("allowedOrgIds", allowed);
        return m;
    }
}
