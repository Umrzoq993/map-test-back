package com.agri.mapapp.auth;

import com.agri.mapapp.audit.AuditService;
import com.agri.mapapp.org.OrganizationUnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final OrganizationUnitRepository orgUnitRepo; // OrganizationUnit
    private final AuditService audit;

    public Page<UserRes> search(String q, Role role, UserStatus status, Long orgId, String dept, Pageable pageable) {
        // q ni oldindan lower + wildcard bilan tayyorlaymiz (masalan: "%ali%")
        String qPattern = (q == null || q.isBlank())
                ? null
                : "%" + q.toLowerCase(Locale.ROOT) + "%";

        return users.search(qPattern, role, status, orgId, dept, pageable).map(this::toRes);
    }

    @Transactional
    public UserRes create(CreateUserReq req, String actorDevice, String actorIp, String actorUa) {
        AppUser u = new AppUser();
        u.setUsername(req.getUsername());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole(req.getRole() != null ? req.getRole() : Role.USER);
        u.setStatus(UserStatus.ACTIVE);

        u.setFullName(req.getFullName());
        u.setPosition(req.getPosition());
        u.setTitle(req.getTitle());
        u.setPhone(req.getPhone());
        u.setAvatarUrl(req.getAvatarUrl());
        u.setDepartment(req.getDepartment());

        if (req.getOrgId() != null && orgUnitRepo != null) {
            u.setOrgUnit(orgUnitRepo.findById(req.getOrgId()).orElse(null));
        }

        users.save(u);
        audit.log("USER_CREATE", u, actorDevice, actorIp, actorUa);
        return toRes(u);
    }

    @Transactional
    public UserRes update(Long id, UpdateUserReq req, String actorDevice, String actorIp, String actorUa) {
        AppUser u = users.findById(id).orElseThrow();
        if (req.getRole() != null) u.setRole(req.getRole());

        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getPosition() != null) u.setPosition(req.getPosition());
        if (req.getTitle() != null) u.setTitle(req.getTitle());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) u.setAvatarUrl(req.getAvatarUrl());
        if (req.getDepartment() != null) u.setDepartment(req.getDepartment());

        if (req.getOrgId() != null && orgUnitRepo != null) {
            u.setOrgUnit(orgUnitRepo.findById(req.getOrgId()).orElse(null));
        }

        users.save(u);
        audit.log("USER_UPDATE", u, actorDevice, actorIp, actorUa);
        return toRes(u);
    }

    @Transactional
    public UserRes changeStatus(Long id, UserStatus status, String actorDevice, String actorIp, String actorUa) {
        AppUser u = users.findById(id).orElseThrow();
        u.setStatus(status);
        users.save(u);
        audit.log("USER_STATUS", u, actorDevice, actorIp, actorUa);
        return toRes(u);
    }

    @Transactional
    public UserRes move(Long id, Long orgId, String dept, String actorDevice, String actorIp, String actorUa) {
        AppUser u = users.findById(id).orElseThrow();
        if (orgUnitRepo != null && orgId != null) {
            u.setOrgUnit(orgUnitRepo.findById(orgId).orElse(null));
        }
        u.setDepartment(dept);
        users.save(u);
        audit.log("USER_MOVE", u, actorDevice, actorIp, actorUa);
        return toRes(u);
    }

    @Transactional
    public ResetPasswordRes resetPassword(Long id, String actorDevice, String actorIp, String actorUa) {
        AppUser u = users.findById(id).orElseThrow();
        String temp = genTempPassword();
        u.setPassword(encoder.encode(temp));
        users.save(u);
        audit.log("USER_RESET_PASSWORD", u, actorDevice, actorIp, actorUa);
        return new ResetPasswordRes(temp);
    }

    private String genTempPassword() {
        String alpha = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?#&";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(alpha.charAt(r.nextInt(alpha.length())));
        return sb.toString();
    }

    private UserRes toRes(AppUser u) {
        return UserRes.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole())
                .status(u.getStatus())
                .fullName(u.getFullName())
                .position(u.getPosition())
                .title(u.getTitle())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .orgId(u.getOrgUnit() != null ? u.getOrgUnit().getId() : null)
                .orgName(u.getOrgUnit() != null ? u.getOrgUnit().getName() : null)
                .department(u.getDepartment())
                .build();
    }
}
