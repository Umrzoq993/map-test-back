package com.agri.mapapp.org;

import com.agri.mapapp.facility.Facility;
import com.agri.mapapp.facility.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrganizationLocateService {

    private final OrganizationUnitRepository orgRepo;
    private final FacilityRepository facilityRepo;

    /** Eski bir parametrli versiya — orqaga moslik uchun qoldiramiz */
    public OrgLocateResponse locateByCode(String code) {
        return locateByCode(code, null);
    }

    /** YANGI: allowed scope bilan (non-admin foydalanuvchilar uchun) */
    public OrgLocateResponse locateByCode(String code, Set<Long> allowed) {
        OrganizationUnit org = orgRepo.findByCode(code.trim())
                .orElseThrow(() -> new NoSuchElementException("Org not found by code: " + code));

        // Agar scope berilgan bo‘lsa va foydalanuvchi ushbu org’ga kira olmasa — 404/deny
        if (allowed != null && !allowed.contains(org.getId())) {
            throw new NoSuchElementException("Org not accessible by code: " + code);
        }

        // Avlodlarini IDs ko‘rinishida yig‘ish
        List<Long> orgIds = new ArrayList<>();
        collectDescendantsIds(org, orgIds);

        // Scope bo‘lsa, avlodlar ro‘yxatini ham shu scope bo‘yicha cheklaymiz
        if (allowed != null) {
            orgIds.removeIf(id -> !allowed.contains(id));
        }

        // O‘sha org (va allowed bo‘lsa, allowed ichidagi avlodlari) resurslari
        List<Facility> facilities = orgIds.isEmpty()
                ? List.of()
                : facilityRepo.findByOrgIdIn(orgIds);

        int level = getDepth(org);

        OrgLocateResponse resp = new OrgLocateResponse();
        resp.setOrg(OrgLocateResponse.OrgDto.from(org, level));
        resp.setFacilities(facilities.stream().map(OrgLocateResponse.FacilityDto::from).toList());
        return resp;
    }

    private void collectDescendantsIds(OrganizationUnit root, List<Long> acc) {
        Deque<OrganizationUnit> dq = new ArrayDeque<>();
        dq.add(root);
        while (!dq.isEmpty()) {
            OrganizationUnit cur = dq.pop();
            acc.add(cur.getId());
            List<OrganizationUnit> children = orgRepo.findByParentOrderBySortOrderAscNameAsc(cur);
            dq.addAll(children);
        }
    }

    private int getDepth(OrganizationUnit node) {
        int d = 0;
        OrganizationUnit p = node.getParent();
        while (p != null) { d++; p = p.getParent(); }
        return d;
    }
}
