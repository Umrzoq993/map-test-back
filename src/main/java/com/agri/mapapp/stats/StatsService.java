package com.agri.mapapp.stats;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import com.agri.mapapp.facility.FacilityStatus;
import com.agri.mapapp.facility.FacilityType;
import com.agri.mapapp.stats.dto.*;
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

    // ✅ Faqat ko‘rsatiladigan (biz belgilagan) turlar
    private static final EnumSet<FacilityType> ALLOWED = EnumSet.of(
            FacilityType.GREENHOUSE,
            FacilityType.POULTRY_MEAT,
            FacilityType.POULTRY_EGG,
            FacilityType.TURKEY,
            FacilityType.COWSHED,
            FacilityType.SHEEPFOLD,
            FacilityType.WORKSHOP_SAUSAGE,
            FacilityType.WORKSHOP_COOKIE,
            FacilityType.AUX_LAND,
            FacilityType.BORDER_LAND,
            FacilityType.FISHPOND
    );

    public OverviewRes overviewAdvanced(Set<Long> allowedOrgIds, Integer year,
                                        List<FacilityType> types,
                                        String range, Integer quarter,
                                        LocalDate from, LocalDate to) {

        if (range == null || range.isBlank()) range = "year";
        range = range.toLowerCase(Locale.ROOT);

        int y = (year != null) ? year : Year.now().getValue();
        LocalDateTime winStart, winEnd;

        if ("quarter".equals(range)) {
            int q = (quarter != null && quarter >= 1 && quarter <= 4) ? quarter : 1;
            int startMonth = (q - 1) * 3 + 1;
            Month sm = Month.of(startMonth);
            Month em = Month.of(startMonth + 2);
            winStart = LocalDate.of(y, sm, 1).atStartOfDay();
            winEnd = LocalDate.of(y, em, em.length(Year.isLeap(y))).plusDays(1).atStartOfDay();
        } else if ("custom".equals(range) && from != null && to != null && !to.isBefore(from)) {
            winStart = from.atStartOfDay();
            winEnd = to.plusDays(1).atStartOfDay();
        } else {
            range = "year";
            winStart = LocalDate.of(y, 1, 1).atStartOfDay();
            winEnd = LocalDate.of(y + 1, 1, 1).atStartOfDay();
        }

        // Base spec + org + ✅ ALLOWED
        Specification<Facility> spec = Specification.where(alwaysTrue())
                .and(typeIn(new ArrayList<>(ALLOWED)));
        if (allowedOrgIds != null) {
            if (allowedOrgIds.isEmpty()) return emptyOverviewForWindow(y, types, range, quarter, winStart, winEnd);
            spec = spec.and(orgIn(allowedOrgIds));
        }

        // Frontdan kelgan turlarni ALLOWED bilan kesishma qilamiz
        if (types != null && !types.isEmpty()) {
            List<FacilityType> onlyAllowed = types.stream().filter(ALLOWED::contains).toList();
            if (onlyAllowed.isEmpty())
                return emptyOverviewForWindow(y, types, range, quarter, winStart, winEnd);
            spec = spec.and(typeIn(onlyAllowed));
        }

        long total = facilityRepo.count(spec);
        long active = facilityRepo.count(spec.and(statusEq(FacilityStatus.ACTIVE)));

        List<Facility> base = facilityRepo.findAll(spec);
        List<Facility> inWindow = facilityRepo.findAll(spec.and(createdBetween(winStart, winEnd)));

        // Type bo‘yicha soni
        Map<FacilityType, Long> byType = base.stream()
                .collect(Collectors.groupingBy(Facility::getType, Collectors.counting()));
        List<TypeAgg> typesAgg = byType.entrySet().stream()
                .map(e -> new TypeAgg(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(TypeAgg::getCount).reversed())
                .toList();

        // Joriy yil oylari
        LocalDateTime yyyyStart = LocalDate.of(y, 1, 1).atStartOfDay();
        LocalDateTime yyyyEnd = LocalDate.of(y + 1, 1, 1).atStartOfDay();
        List<Facility> thisYear = facilityRepo.findAll(spec.and(createdBetween(yyyyStart, yyyyEnd)));
        long[] monthBuckets = new long[12];
        for (Facility f : thisYear) {
            LocalDateTime c = f.getCreatedAt();
            if (c != null && c.getYear() == y) monthBuckets[c.getMonthValue() - 1]++;
        }
        List<MonthlyCount> monthly = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) monthly.add(new MonthlyCount(i + 1, monthBuckets[i]));
        long yearNewCount = Arrays.stream(monthBuckets).sum();

        // O‘tgan yil
        int py = y - 1;
        LocalDateTime prevStart = LocalDate.of(py, 1, 1).atStartOfDay();
        LocalDateTime prevEnd = LocalDate.of(y, 1, 1).atStartOfDay();
        List<Facility> prevYear = facilityRepo.findAll(spec.and(createdBetween(prevStart, prevEnd)));
        long[] prevBuckets = new long[12];
        for (Facility f : prevYear) {
            LocalDateTime c = f.getCreatedAt();
            if (c != null && c.getYear() == py) prevBuckets[c.getMonthValue() - 1]++;
        }
        List<MonthlyCount> prevMonthly = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) prevMonthly.add(new MonthlyCount(i + 1, prevBuckets[i]));
        long prevYearNewCount = Arrays.stream(prevBuckets).sum();
        Double yoyNewPct = (prevYearNewCount > 0) ? ((yearNewCount - (double) prevYearNewCount) * 100.0 / prevYearNewCount) : null;

        // Moliyaviy (joriy va o‘tgan yil)
        double[] revYY = new double[12], profYY = new double[12];
        for (Facility f : thisYear) {
            LocalDateTime c = f.getCreatedAt();
            if (c == null || c.getYear() != y) continue;
            int m = c.getMonthValue() - 1;
            revYY[m] += attrD(f, "revenue");
            profYY[m] += attrD(f, "profit");
        }
        List<MonthlyAmount> revenueMonthly = new ArrayList<>(12);
        List<MonthlyAmount> profitMonthly = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            revenueMonthly.add(new MonthlyAmount(i + 1, revYY[i]));
            profitMonthly.add(new MonthlyAmount(i + 1, profYY[i]));
        }

        double[] revPY = new double[12], profPY = new double[12];
        for (Facility f : prevYear) {
            LocalDateTime c = f.getCreatedAt();
            if (c == null || c.getYear() != py) continue;
            int m = c.getMonthValue() - 1;
            revPY[m] += attrD(f, "revenue");
            profPY[m] += attrD(f, "profit");
        }
        List<MonthlyAmount> revenuePrevMonthly = new ArrayList<>(12);
        List<MonthlyAmount> profitPrevMonthly = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            revenuePrevMonthly.add(new MonthlyAmount(i + 1, revPY[i]));
            profitPrevMonthly.add(new MonthlyAmount(i + 1, profPY[i]));
        }

        // YTD (tanlangan oynada)
        double revenueYtd = inWindow.stream().mapToDouble(f -> attrD(f, "revenue")).sum();
        double profitYtd = inWindow.stream().mapToDouble(f -> attrD(f, "profit")).sum();

        // YTD o‘tgan yil xuddi shu oynada -> YoY
        LocalDateTime winStartPrev = winStart.minusYears(1);
        LocalDateTime winEndPrev = winEnd.minusYears(1);
        List<Facility> inPrevWindow = facilityRepo.findAll(spec.and(createdBetween(winStartPrev, winEndPrev)));
        double revenuePrevYtd = inPrevWindow.stream().mapToDouble(f -> attrD(f, "revenue")).sum();
        double profitPrevYtd = inPrevWindow.stream().mapToDouble(f -> attrD(f, "profit")).sum();
        Double yoyRevenuePct = (revenuePrevYtd > 0) ? ((revenueYtd - revenuePrevYtd) * 100.0 / revenuePrevYtd) : null;
        Double yoyProfitPct = (profitPrevYtd > 0) ? ((profitYtd - profitPrevYtd) * 100.0 / profitPrevYtd) : null;

        // Org bo‘yicha soni
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

        // Sig'im / mahsuldorlik (bazada)
        double sumCurrent = 0.0, sumCapacity = 0.0, sumProductKg = 0.0, sumAreaM2 = 0.0;
        for (Facility f : base) {
            sumCurrent += attrD(f, "current");
            sumCapacity += attrD(f, "capacity");
            sumProductKg += attrD(f, "product_kg");
            sumAreaM2 += attrD(f, "area_m2");
        }
        Double capacityUtilPct = (sumCapacity > 0) ? (sumCurrent * 100.0 / sumCapacity) : null;
        Double productivityKgPerM2 = (sumAreaM2 > 0) ? (sumProductKg / sumAreaM2) : null;

        // Type KPI (oyna ichida)
        Map<FacilityType, TypeKpiAgg> typeKpisMap = new EnumMap<>(FacilityType.class);
        for (Facility f : inWindow) {
            FacilityType t = f.getType();
            if (t == null) continue;
            TypeKpiAgg agg = typeKpisMap.computeIfAbsent(t,
                    k -> new TypeKpiAgg(k, 0L, 0.0, 0.0, 0.0, 0.0, null));
            agg.setCount(agg.getCount() + 1);
            agg.setRevenue(agg.getRevenue() + attrD(f, "revenue"));
            agg.setProfit(agg.getProfit() + attrD(f, "profit"));
            agg.setCurrent(agg.getCurrent() + attrD(f, "current"));
            agg.setCapacity(agg.getCapacity() + attrD(f, "capacity"));
        }
        List<TypeKpiAgg> typeKpis = typeKpisMap.values().stream()
                .peek(a -> a.setUtilPct(a.getCapacity() > 0 ? (a.getCurrent() * 100.0 / a.getCapacity()) : null))
                .sorted(Comparator.comparingDouble((TypeKpiAgg a) -> a.getRevenue() != null ? a.getRevenue() : 0.0).reversed())
                .toList();

        // TOP-10 org tushum bo‘yicha (oyna ichida)
        Map<Long, OrgRevenueAgg> byOrgRev = new HashMap<>();
        for (Facility f : inWindow) {
            if (f.getOrg() == null) continue;
            Long id = f.getOrg().getId();
            OrgRevenueAgg agg = byOrgRev.computeIfAbsent(id, k -> new OrgRevenueAgg(id, safeOrgName(f), 0L, 0.0, 0.0));
            agg.setCount(agg.getCount() + 1);
            agg.setRevenue(agg.getRevenue() + attrD(f, "revenue"));
            agg.setProfit(agg.getProfit() + attrD(f, "profit"));
        }
        List<OrgRevenueAgg> topOrgRevenue = byOrgRev.values().stream()
                .sorted(Comparator.comparingDouble(OrgRevenueAgg::getRevenue).reversed())
                .limit(10)
                .toList();

        // Javob
        OverviewRes res = new OverviewRes();
        res.setTotal(total);
        res.setActive(active);
        res.setTypes(typesAgg);
        res.setMonthly(monthly);
        res.setPrevMonthly(prevMonthly);
        res.setYearNewCount(yearNewCount);
        res.setPrevYearNewCount(prevYearNewCount);
        res.setYoyNewPct(yoyNewPct);

        res.setRevenueMonthly(revenueMonthly);
        res.setRevenuePrevMonthly(revenuePrevMonthly);
        res.setRevenueYtd(revenueYtd);
        res.setRevenuePrevYtd(revenuePrevYtd);
        res.setYoyRevenuePct(yoyRevenuePct);

        res.setProfitMonthly(profitMonthly);
        res.setProfitPrevMonthly(profitPrevMonthly);
        res.setProfitYtd(profitYtd);
        res.setProfitPrevYtd(profitPrevYtd);
        res.setYoyProfitPct(yoyProfitPct);

        res.setCapacityUtilPct(capacityUtilPct);
        res.setProductivityKgPerM2(productivityKgPerM2);
        res.setTypeKpis(typeKpis);
        res.setTopOrgRevenue(topOrgRevenue);
        res.setOrgs(orgs);

        res.setYear(y);
        res.setTypesFilter(types);
        res.setRange(range);
        res.setQuarter(quarter);
        if ("custom".equals(range)) {
            res.setFrom(winStart.toLocalDate().toString());
            res.setTo(winEnd.minusDays(1).toLocalDate().toString());
        }
        return res;
    }

    public String exportCsv(Set<Long> allowedOrgIds, Integer year,
                            List<FacilityType> types,
                            String range, Integer quarter,
                            LocalDate from, LocalDate to) {

        if (range == null || range.isBlank()) range = "year";
        range = range.toLowerCase(Locale.ROOT);

        int y = (year != null) ? year : Year.now().getValue();
        LocalDateTime winStart, winEnd;
        if ("quarter".equals(range)) {
            int q = (quarter != null && quarter >= 1 && quarter <= 4) ? quarter : 1;
            int startMonth = (q - 1) * 3 + 1;
            Month sm = Month.of(startMonth);
            Month em = Month.of(startMonth + 2);
            winStart = LocalDate.of(y, sm, 1).atStartOfDay();
            winEnd = LocalDate.of(y, em, em.length(Year.isLeap(y))).plusDays(1).atStartOfDay();
        } else if ("custom".equals(range) && from != null && to != null && !to.isBefore(from)) {
            winStart = from.atStartOfDay();
            winEnd = to.plusDays(1).atStartOfDay();
        } else {
            winStart = LocalDate.of(y, 1, 1).atStartOfDay();
            winEnd = LocalDate.of(y + 1, 1, 1).atStartOfDay();
        }

        Specification<Facility> spec = Specification.where(alwaysTrue())
                .and(typeIn(new ArrayList<>(ALLOWED)));
        if (allowedOrgIds != null) {
            if (allowedOrgIds.isEmpty()) return "orgId,orgName,type,createdAt,revenue,profit,current,capacity,product_kg,area_m2\n";
            spec = spec.and(orgIn(allowedOrgIds));
        }
        if (types != null && !types.isEmpty()) {
            List<FacilityType> onlyAllowed = types.stream().filter(ALLOWED::contains).toList();
            if (onlyAllowed.isEmpty())
                return "orgId,orgName,type,createdAt,revenue,profit,current,capacity,product_kg,area_m2\n";
            spec = spec.and(typeIn(onlyAllowed));
        }

        List<Facility> inWindow = facilityRepo.findAll(spec.and(createdBetween(winStart, winEnd)));

        StringBuilder sb = new StringBuilder();
        sb.append("orgId,orgName,type,createdAt,revenue,profit,current,capacity,product_kg,area_m2\n");
        for (Facility f : inWindow) {
            Long orgId = (f.getOrg() != null) ? f.getOrg().getId() : null;
            String orgName = (f.getOrg() != null) ? safeOrgName(f) : "";
            String type = (f.getType() != null) ? f.getType().name() : "";
            String created = (f.getCreatedAt() != null) ? f.getCreatedAt().toString() : "";
            double revenue = attrD(f, "revenue");
            double profit = attrD(f, "profit");
            double current = attrD(f, "current");
            double capacity = attrD(f, "capacity");
            double productKg = attrD(f, "product_kg");
            double areaM2 = attrD(f, "area_m2");
            sb.append(csv(orgId)).append(',')
                    .append(csv(orgName)).append(',')
                    .append(csv(type)).append(',')
                    .append(csv(created)).append(',')
                    .append(revenue).append(',')
                    .append(profit).append(',')
                    .append(current).append(',')
                    .append(capacity).append(',')
                    .append(productKg).append(',')
                    .append(areaM2).append('\n');
        }
        return sb.toString();
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
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

    @SuppressWarnings("unchecked")
    private double attrD(Facility f, String key) {
        try {
            Object attrs = f.getAttributes();
            if (attrs instanceof Map<?, ?> map) {
                Object v = map.get(key);
                return num(v);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }
    private double num(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) { try { return Double.parseDouble(s.trim()); } catch (Exception ignored) {} }
        return 0.0;
    }
    private String safeOrgName(Facility f) {
        try { return f.getOrg().getName(); } catch (Exception e) { return "—"; }
    }

    private OverviewRes emptyOverviewForWindow(Integer year, List<FacilityType> types,
                                               String range, Integer quarter,
                                               LocalDateTime winStart, LocalDateTime winEnd) {
        OverviewRes res = new OverviewRes();
        res.setTotal(0L);
        res.setActive(0L);
        res.setTypes(List.of());
        List<MonthlyCount> zero = Arrays.asList(
                new MonthlyCount(1,0), new MonthlyCount(2,0), new MonthlyCount(3,0),
                new MonthlyCount(4,0), new MonthlyCount(5,0), new MonthlyCount(6,0),
                new MonthlyCount(7,0), new MonthlyCount(8,0), new MonthlyCount(9,0),
                new MonthlyCount(10,0), new MonthlyCount(11,0), new MonthlyCount(12,0)
        );
        res.setMonthly(zero);
        res.setPrevMonthly(zero);
        res.setYearNewCount(0L);
        res.setPrevYearNewCount(0L);
        res.setYoyNewPct(null);
        res.setRevenueMonthly(List.of());
        res.setRevenuePrevMonthly(List.of());
        res.setRevenueYtd(0.0);
        res.setRevenuePrevYtd(0.0);
        res.setYoyRevenuePct(null);
        res.setProfitMonthly(List.of());
        res.setProfitPrevMonthly(List.of());
        res.setProfitYtd(0.0);
        res.setProfitPrevYtd(0.0);
        res.setYoyProfitPct(null);
        res.setCapacityUtilPct(null);
        res.setProductivityKgPerM2(null);
        res.setTypeKpis(List.of());
        res.setTopOrgRevenue(List.of());
        res.setOrgs(List.of());

        int y = (year != null) ? year : Year.now().getValue();
        res.setYear(y);
        res.setTypesFilter(types);
        res.setRange(range);
        res.setQuarter(quarter);
        if ("custom".equals(range)) {
            res.setFrom(winStart.toLocalDate().toString());
            res.setTo(winEnd.minusDays(1).toLocalDate().toString());
        }
        return res;
    }
}
