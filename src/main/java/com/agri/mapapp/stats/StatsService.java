package com.agri.mapapp.stats;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.agri.mapapp.stats.dto.MonthlyCount;
import com.agri.mapapp.stats.dto.OrgAgg;
import com.agri.mapapp.stats.dto.OverviewRes;
import com.agri.mapapp.stats.dto.TypeAgg;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final FacilityRepository facilityRepo;

    public OverviewRes overview(Set<Long> allowedOrgIds, Integer year, List<FacilityType> types) {
        // 1) Bazaviy filter (org scoping + types)
        Specification<Facility> spec = Specification.where(alwaysTrue());
        if (allowedOrgIds != null) {
            // ADMIN bo'lmasa (null emas) — agar bo'sh bo'lsa natija ham bo'sh
            if (allowedOrgIds.isEmpty()) {
                return emptyOverview(year, types);
            }
            spec = spec.and(orgIn(allowedOrgIds));
        }
        if (types != null && !types.isEmpty()) {
            spec = spec.and(typeIn(types));
        }

        // 2) Total va Active
        long total = facilityRepo.count(spec);
        long active = facilityRepo.count(spec.and(statusEq(FacilityStatus.ACTIVE)));

        // 3) Bazaviy ro'yxat (org/types filtrlari bilan)
        List<Facility> base = facilityRepo.findAll(spec);

        // 4) Type bo'yicha agg
        Map<FacilityType, Long> byType = base.stream()
                .collect(Collectors.groupingBy(Facility::getType, Collectors.counting()));
        List<TypeAgg> typesAgg = byType.entrySet().stream()
                .map(e -> new TypeAgg(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(TypeAgg::getCount).reversed())
                .toList();

        // 5) Year + oylar bo'yicha (createdAt mavjudlariga)
        int y = (year != null) ? year : Year.now().getValue();
        LocalDateTime from = LocalDate.of(y, 1, 1).atStartOfDay();
        LocalDateTime to = from.plusYears(1);
        List<Facility> withinYear = facilityRepo.findAll(spec.and(createdBetween(from, to)));

        long[] monthBuckets = new long[12];
        for (Facility f : withinYear) {
            if (f.getCreatedAt() != null && f.getCreatedAt().getYear() == y) {
                int m = f.getCreatedAt().getMonthValue();
                monthBuckets[m - 1]++;
            }
        }
        List<MonthlyCount> monthly = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            monthly.add(new MonthlyCount(i + 1, monthBuckets[i]));
        }

        // 6) Org bo'yicha agg (nom bilan)
        Map<Long, Long> cntPerOrg = new HashMap<>();
        Map<Long, String> namePerOrg = new HashMap<>();
        for (Facility f : base) {
            if (f.getOrg() == null) continue;
            Long id = f.getOrg().getId();
            cntPerOrg.merge(id, 1L, Long::sum);
            namePerOrg.putIfAbsent(id, safeOrgName(f));
        }
        List<OrgAgg> orgs = cntPerOrg.entrySet().stream()
                .map(e -> new OrgAgg(e.getKey(), namePerOrg.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(OrgAgg::getCount).reversed())
                .toList();

        // 7) Javob
        OverviewRes res = new OverviewRes();
        res.setTotal(total);
        res.setActive(active);
        res.setTypes(typesAgg);
        res.setMonthly(monthly);
        res.setOrgs(orgs);
        // Agar OverviewRes’da year/types fields bo‘lsa, quyidagilarni qo‘yishingiz mumkin:
        try { res.getClass().getMethod("setYear", Integer.class); res.setYear(y); } catch (Exception ignored) {}
        try { res.getClass().getMethod("setTypesFilter", List.class); res.setTypesFilter(types); } catch (Exception ignored) {}
        return res;
    }

    /* -------------------- Specification helpers -------------------- */

    private Specification<Facility> alwaysTrue() {
        return (root, cq, cb) -> cb.conjunction();
    }

    private Specification<Facility> orgIn(Set<Long> ids) {
        return (root, cq, cb) -> root.get("org").get("id").in(ids);
    }

    private Specification<Facility> typeIn(List<FacilityType> types) {
        return (root, cq, cb) -> root.get("type").in(types);
    }

    private Specification<Facility> statusEq(FacilityStatus st) {
        return (root, cq, cb) -> cb.equal(root.get("status"), st);
    }

    private Specification<Facility> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, cq, cb) -> cb.between(root.get("createdAt"), from, to);
    }

    private OverviewRes emptyOverview(Integer year, List<FacilityType> types) {
        OverviewRes res = new OverviewRes();
        res.setTotal(0L);
        res.setActive(0L);
        res.setTypes(List.of());
        res.setMonthly(Arrays.asList(
                new MonthlyCount(1,0), new MonthlyCount(2,0), new MonthlyCount(3,0),
                new MonthlyCount(4,0), new MonthlyCount(5,0), new MonthlyCount(6,0),
                new MonthlyCount(7,0), new MonthlyCount(8,0), new MonthlyCount(9,0),
                new MonthlyCount(10,0), new MonthlyCount(11,0), new MonthlyCount(12,0)
        ));
        res.setOrgs(List.of());
        try { res.getClass().getMethod("setYear", Integer.class); res.setYear((year!=null)?year:Year.now().getValue()); } catch (Exception ignored) {}
        try { res.getClass().getMethod("setTypesFilter", List.class); res.setTypesFilter(types); } catch (Exception ignored) {}
        return res;
    }

    private String safeOrgName(Facility f) {
        try { return f.getOrg().getName(); } catch (Exception e) { return "—"; }
    }
}
