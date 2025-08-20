package com.agri.mapapp.org;

import com.agri.mapapp.auth.Role;
import com.agri.mapapp.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final OrganizationUnitRepository repo;

    /** Foydalanuvchi ko‘ra oladigan orgId’lar to‘plami (o‘zi + barcha avlodlari) */
    public Set<Long> allowedOrgIds(Authentication auth) {
        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        if (up.getRole() == Role.ADMIN) return null; // null -> cheklanmagan (ADMIN)
        Long rootId = up.getOrgId();
        if (rootId == null) return Set.of(); // org biriktirilmagan ORG_USER
        // barcha orglarni olib, subtree ni hisoblaymiz (oddiy DFS)
        List<OrganizationUnit> all = repo.findAll();
        Map<Long, List<Long>> children = new HashMap<>();
        for (OrganizationUnit u : all) {
            Long pid = (u.getParent() != null) ? u.getParent().getId() : null;
            if (pid != null) children.computeIfAbsent(pid, k -> new ArrayList<>()).add(u.getId());
        }
        Set<Long> out = new HashSet<>();
        Deque<Long> st = new ArrayDeque<>();
        st.push(rootId); out.add(rootId);
        int guard = 0;
        while (!st.isEmpty() && guard++ < 100000) {
            Long cur = st.pop();
            for (Long ch : children.getOrDefault(cur, List.of())) {
                if (out.add(ch)) st.push(ch);
            }
        }
        return out;
    }

    /** Tekshir: mazkur orgId foydalanuvchining ruxsat doirasida bormi? (ADMIN -> true) */
    public boolean canAccessOrg(Authentication auth, Long orgId) {
        if (orgId == null) return false;
        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        if (up.getRole() == Role.ADMIN) return true;
        Set<Long> ids = allowedOrgIds(auth);
        return ids.contains(orgId);
    }
}
