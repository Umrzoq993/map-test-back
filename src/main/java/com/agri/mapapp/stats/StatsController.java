package com.agri.mapapp.stats;

import com.agri.mapapp.facility.FacilityType;
import com.agri.mapapp.org.AccessService;
import com.agri.mapapp.stats.dto.OverviewRes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final AccessService access;

    @GetMapping("/overview")
    public OverviewRes overview(
            Authentication auth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String types // CSV: "COWSHED,POULTRY"
    ) {
        // ADMIN => null (cheklanmagan), ORG_USER => o‘zi + avlodlari to‘plami
        Set<Long> allowed = access.allowedOrgIds(auth);
        List<FacilityType> typeList = parseTypes(types);
        return statsService.overview(allowed, year, typeList);
    }

    private List<FacilityType> parseTypes(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(FacilityType::valueOf)
                .collect(Collectors.toList());
    }
}
