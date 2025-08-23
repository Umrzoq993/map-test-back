package com.agri.mapapp.org;

import com.agri.mapapp.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static com.agri.mapapp.org.OrganizationSpecs.*;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationUnitRepository repo;

    public Optional<OrganizationUnit> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return repo.findByCode(code.trim());
    }

    /** Tree (sizdagi mavjud implementatsiya) — query-methodsiz ham bo‘ladi */
    public List<OrgNodeDto> getOrgTree() {
        // findAll(Sort) orqali: sortOrder ASC, name ASC
        List<OrganizationUnit> all = repo.findAll(Sort.by(
                Sort.Order.asc("sortOrder"),
                Sort.Order.asc("name")
        ));

        Map<Long, OrgNodeDto> map = new HashMap<>();
        List<OrgNodeDto> roots = new ArrayList<>();

        for (OrganizationUnit u : all) {
            OrgNodeDto dto = OrgNodeDto.builder()
                    .key(String.valueOf(u.getId()))
                    .title(u.getName())
                    .zoom(u.getZoom())
                    .pos(u.getLat() != null && u.getLng() != null ? new double[]{u.getLat(), u.getLng()} : null)
                    .build();
            map.put(u.getId(), dto);
        }
        for (OrganizationUnit u : all) {
            OrgNodeDto dto = map.get(u.getId());
            if (u.getParent() != null) {
                OrgNodeDto parentDto = map.get(u.getParent().getId());
                if (parentDto != null) parentDto.getChildren().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }

    // ---------- Helpers (CRUD uchun ham foydali) ----------

    private OrganizationUnit getOr404(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Org not found: " + id));
    }

    private int depthOf(OrganizationUnit u) {
        int d = 0, guard = 0;
        OrganizationUnit cur = u;
        while (cur != null && guard++ < 10000) {
            cur = cur.getParent();
            if (cur != null) d++;
        }
        return d;
    }

    private List<OrganizationUnit> siblingsOf(OrganizationUnit parent) {
        Specification<OrganizationUnit> spec = (parent == null) ? parentIsNull() : parentEq(parent.getId());
        return repo.findAll(spec, Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")));
    }

    private int clamp(int val, int min, int max) { return Math.max(min, Math.min(max, val)); }

    private boolean hasChildren(OrganizationUnit parent) {
        Specification<OrganizationUnit> spec = (parent == null) ? parentIsNull() : parentEq(parent.getId());
        // parent = X bo‘lgan bolalar soni > 0 bo‘lsa true
        return repo.count(spec) > 0;
    }

    private boolean isDescendant(OrganizationUnit candidate, OrganizationUnit root) {
        if (candidate == null || root == null) return false;
        int guard = 0;
        OrganizationUnit cur = candidate;
        while (cur != null && guard++ < 10000) {
            if (Objects.equals(cur.getId(), root.getId())) return true;
            cur = cur.getParent();
        }
        return false;
    }

    // ---------- PAGINATION + SORT (Specification bilan) ----------

    public PageResponse<OrgFlatRes> findPageForUser(String q, Long parentId, Pageable pageable, Set<Long> allowed) {
        // bazaviy spec
        Specification<OrganizationUnit> spec = Specification.where(OrganizationSpecs.nameLike(q));
        if (parentId != null) spec = spec.and(OrganizationSpecs.parentEq(parentId));
        if (allowed != null) spec = spec.and(OrganizationSpecs.idIn(allowed));

        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name"));
        }
        Pageable pageReq = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<OrganizationUnit> page = repo.findAll(spec, pageReq);

        // parent names & hasChildren/ depth — SIZNING MANTIQ O‘ZGARMASIN
        List<OrgFlatRes> list = new ArrayList<>();
        Set<Long> parentIds = new HashSet<>();
        for (var u : page.getContent()) if (u.getParent() != null) parentIds.add(u.getParent().getId());
        Map<Long, String> parentNames = new HashMap<>();
        if (!parentIds.isEmpty()) {
            for (var p : repo.findAllById(parentIds)) parentNames.put(p.getId(), p.getName());
        }
        Set<Long> contentIds = new HashSet<>();
        for (var u : page.getContent()) contentIds.add(u.getId());
        var childSpec = OrganizationSpecs.parentIn(contentIds);
        var childs = repo.findAll(childSpec);
        Set<Long> hasChildIds = new HashSet<>();
        for (var c : childs) if (c.getParent() != null) hasChildIds.add(c.getParent().getId());

        for (var u : page.getContent()) {
            String pName = (u.getParent() != null) ? parentNames.get(u.getParent().getId()) : null;
            int depth = depthOf(u);
            boolean hc = hasChildIds.contains(u.getId());
            list.add(OrgFlatRes.of(u, pName, depth, hc));
        }

        return PageResponse.of(new PageImpl<>(list, pageReq, page.getTotalElements()));
    }


    public java.util.List<OrgNodeDto> getOrgTreeForUser(Set<Long> allowed) {
        if (allowed == null) return getOrgTree(); // ADMIN
        // faqat allowed idlarni daraxtga solamiz
        var all = repo.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")));
        var map = new java.util.HashMap<Long, OrgNodeDto>();
        var roots = new java.util.ArrayList<OrgNodeDto>();
        for (var u: all) {
            if (!allowed.contains(u.getId())) continue;
            var dto = OrgNodeDto.builder()
                    .key(String.valueOf(u.getId()))
                    .title(u.getName())
                    .zoom(u.getZoom())
                    .pos(u.getLat()!=null && u.getLng()!=null ? new double[]{u.getLat(), u.getLng()} : null)
                    .build();
            map.put(u.getId(), dto);
        }
        for (var u: all) {
            if (!allowed.contains(u.getId())) continue;
            var dto = map.get(u.getId());
            if (u.getParent()!=null && allowed.contains(u.getParent().getId())) {
                var p = map.get(u.getParent().getId());
                if (p!=null) p.getChildren().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public Page<OrgFlatRes> findPage(String q, Long parentId, Pageable pageable) {
        // Spec: name LIKE + (parent == parentId) ixtiyoriy
        Specification<OrganizationUnit> spec = Specification.where(OrganizationSpecs.nameLike(q));
        if (parentId != null) {
            spec = spec.and(OrganizationSpecs.parentEq(parentId));
        }

        // Default sort agar berilmagan bo‘lsa
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name"));
        }
        Pageable pageReq = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<OrganizationUnit> page = repo.findAll(spec, pageReq);
        List<OrganizationUnit> content = page.getContent();

        // id’lar to‘plami
        Set<Long> ids = new HashSet<>();
        for (OrganizationUnit u : content) {
            ids.add(u.getId());
        }

        // hasChildren: parent IN (ids) bo‘lgan bolalarni topib, parentId setini tuzamiz
        Set<Long> hasChildIds = new HashSet<>();
        if (!ids.isEmpty()) {
            Specification<OrganizationUnit> childrenSpec = OrganizationSpecs.parentIn(ids);
            List<OrganizationUnit> children = repo.findAll(childrenSpec);
            for (OrganizationUnit c : children) {
                OrganizationUnit p = c.getParent();
                if (p != null) {
                    hasChildIds.add(p.getId());
                }
            }
        }

        // parent nomlarini xaritalash
        Set<Long> parentIdSet = new HashSet<>();
        for (OrganizationUnit u : content) {
            if (u.getParent() != null) {
                parentIdSet.add(u.getParent().getId());
            }
        }
        Map<Long, String> parentNames = new HashMap<>();
        if (!parentIdSet.isEmpty()) {
            List<OrganizationUnit> parents = repo.findAllById(parentIdSet);
            for (OrganizationUnit p : parents) {
                parentNames.put(p.getId(), p.getName());
            }
        }

        // Map -> DTO
        List<OrgFlatRes> list = new ArrayList<>(content.size());
        for (OrganizationUnit u : content) {
            String pName = (u.getParent() != null) ? parentNames.get(u.getParent().getId()) : null;
            int depth = depthOf(u);
            boolean hc = hasChildIds.contains(u.getId());
            list.add(OrgFlatRes.of(u, pName, depth, hc));
        }

        return new PageImpl<>(list, pageReq, page.getTotalElements());
    }

    // ---------- CREATE / UPDATE / MOVE / DELETE (oldingi mantiq, lekin specs bilan siblings) ----------

    @Transactional
    public OrganizationUnit createOrg(String name, Long parentId, Double lat, Double lng, Integer zoom, Integer sortOrder) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        OrganizationUnit parent = (parentId == null) ? null : getOr404(parentId);

        List<OrganizationUnit> siblings = siblingsOf(parent);
        int targetIdx = (sortOrder == null) ? siblings.size() : clamp(sortOrder, 0, siblings.size());

        for (int i = siblings.size() - 1; i >= targetIdx; i--) {
            OrganizationUnit s = siblings.get(i);
            s.setSortOrder((s.getSortOrder() == null ? i : s.getSortOrder()) + 1);
            repo.save(s);
        }

        OrganizationUnit u = OrganizationUnit.builder()
                .name(name.trim())
                .parent(parent)
                .lat(lat).lng(lng).zoom(zoom)
                .sortOrder(targetIdx)
                .build();

        return repo.save(u);
    }

    @Transactional
    public OrganizationUnit updateOrg(Long id, String name, Double lat, Double lng, Integer zoom, Integer sortOrder) {
        OrganizationUnit u = getOr404(id);
        if (name != null) {
            String nm = name.trim();
            if (nm.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
            u.setName(nm);
        }
        if (lat != null) u.setLat(lat);
        if (lng != null) u.setLng(lng);
        if (zoom != null) u.setZoom(zoom);

        if (sortOrder != null) {
            OrganizationUnit parent = u.getParent();
            List<OrganizationUnit> siblings = siblingsOf(parent);
            int oldIdx = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (Objects.equals(siblings.get(i).getId(), u.getId())) { oldIdx = i; break; }
            }
            if (oldIdx != -1) {
                int newIdx = clamp(sortOrder, 0, siblings.size() - 1);
                if (newIdx != oldIdx) {
                    if (newIdx < oldIdx) {
                        for (int i = siblings.size() - 1; i >= 0; i--) {
                            OrganizationUnit s = siblings.get(i);
                            if (i >= newIdx && i < oldIdx) { s.setSortOrder(i + 1); repo.save(s); }
                        }
                    } else {
                        for (int i = 0; i < siblings.size(); i++) {
                            OrganizationUnit s = siblings.get(i);
                            if (i > oldIdx && i <= newIdx) { s.setSortOrder(i - 1); repo.save(s); }
                        }
                    }
                    u.setSortOrder(newIdx);
                }
            }
        }

        return repo.save(u);
    }

    @Transactional
    public OrganizationUnit moveOrg(Long id, Long newParentId, int orderIndex) {
        OrganizationUnit u = getOr404(id);
        OrganizationUnit oldParent = u.getParent();
        OrganizationUnit newParent = (newParentId == null) ? null : getOr404(newParentId);

        if (Objects.equals(id, newParentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot set parent to itself");
        }
        if (isDescendant(newParent, u)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move into own subtree");
        }

        List<OrganizationUnit> oldSiblings = siblingsOf(oldParent);
        int oldIdx = -1;
        for (int i = 0; i < oldSiblings.size(); i++) {
            if (Objects.equals(oldSiblings.get(i).getId(), id)) { oldIdx = i; break; }
        }
        if (oldIdx != -1) {
            for (int i = 0; i < oldSiblings.size(); i++) {
                OrganizationUnit s = oldSiblings.get(i);
                if (!Objects.equals(s.getId(), id) && i > oldIdx) { s.setSortOrder(i - 1); repo.save(s); }
            }
        }

        List<OrganizationUnit> newSiblings = siblingsOf(newParent);
        int target = clamp(orderIndex, 0, newSiblings.size());
        for (int i = newSiblings.size() - 1; i >= target; i--) {
            OrganizationUnit s = newSiblings.get(i);
            s.setSortOrder(i + 1);
            repo.save(s);
        }

        u.setParent(newParent);
        u.setSortOrder(target);
        return repo.save(u);
    }

    @Transactional
    public void deleteOrg(Long id) {
        OrganizationUnit u = getOr404(id);
        if (hasChildren(u)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete node with children");
        }
        OrganizationUnit parent = u.getParent();
        List<OrganizationUnit> siblings = siblingsOf(parent);
        int idx = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (Objects.equals(siblings.get(i).getId(), id)) { idx = i; break; }
        }
        if (idx != -1) {
            for (int i = 0; i < siblings.size(); i++) {
                OrganizationUnit s = siblings.get(i);
                if (!Objects.equals(s.getId(), id) && i > idx) { s.setSortOrder(i - 1); repo.save(s); }
            }
        }
        repo.delete(u);
    }
}
