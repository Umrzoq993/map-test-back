package com.agri.mapapp.stats;

import com.agri.mapapp.facility.FacilityType;
import com.agri.mapapp.stats.dto.OverviewRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public OverviewRes overview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "year") String range, // year|quarter|custom
            @RequestParam(required = false) Integer quarter,                      // 1..4 if range=quarter
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String types // CSV of FacilityType
    ) {
        // NOTE: allowedOrgIds — kerak bo'lsa securitydan oling; hozircha admin/all
        Set<Long> allowedOrgIds = null;

        List<FacilityType> typeList = null;
        if (types != null && !types.isBlank()) {
            typeList = Arrays.stream(types.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(FacilityType::valueOf)
                    .collect(Collectors.toList());
        }

        return statsService.overviewAdvanced(allowedOrgIds, year, typeList, range, quarter, from, to);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "year") String range,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String types
    ) {
        Set<Long> allowedOrgIds = null;
        List<FacilityType> typeList = null;
        if (types != null && !types.isBlank()) {
            typeList = Arrays.stream(types.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(FacilityType::valueOf)
                    .collect(Collectors.toList());
        }

        String csv = statsService.exportCsv(allowedOrgIds, year, typeList, range, quarter, from, to);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stats-export.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }
}
