package com.agri.mapapp.auth;

import com.agri.mapapp.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService users;

    @GetMapping
    public ResponseEntity<PageResponse<UserRes>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String department
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<UserRes> pg = users.search(q, role, status, orgId, department, pageable);
        return ResponseEntity.ok(new PageResponse<>(
                pg.getContent(), pg.getNumber(), pg.getSize(), pg.getTotalElements(), pg.getTotalPages(), pg.isLast()
        ));
    }

    @GetMapping("/online")
    public ResponseEntity<PageResponse<UserRes>> online(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<UserRes> pg = users.online(pageable);
        return ResponseEntity.ok(new PageResponse<>(
                pg.getContent(), pg.getNumber(), pg.getSize(), pg.getTotalElements(), pg.getTotalPages(), pg.isLast()
        ));
    }

    @PostMapping
    public ResponseEntity<UserRes> create(@RequestBody CreateUserReq req, HttpServletRequest r) {
        return ResponseEntity.ok(users.create(req, device(r), ip(r), ua(r)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserRes> update(@PathVariable Long id, @RequestBody UpdateUserReq req, HttpServletRequest r) {
        return ResponseEntity.ok(users.update(id, req, device(r), ip(r), ua(r)));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<UserRes> status(@PathVariable Long id, @RequestBody StatusReq req, HttpServletRequest r) {
        return ResponseEntity.ok(users.changeStatus(id, req.getStatus(), device(r), ip(r), ua(r)));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<UserRes> move(@PathVariable Long id, @RequestBody MoveReq req, HttpServletRequest r) {
        return ResponseEntity.ok(users.move(id, req.getOrgId(), req.getDepartment(), device(r), ip(r), ua(r)));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ResetPasswordRes> reset(@PathVariable Long id, HttpServletRequest r) {
        return ResponseEntity.ok(users.resetPassword(id, device(r), ip(r), ua(r)));
    }

    // ------- helpers -------
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("id"));
        String[] p = sort.split(",");
        String f = p[0].trim();
        String d = p.length > 1 ? p[1].trim().toLowerCase() : "desc";
        return d.startsWith("asc") ? Sort.by(Sort.Order.asc(f)) : Sort.by(Sort.Order.desc(f));
    }
    private String device(HttpServletRequest r) {
        String v = r.getHeader("X-Device-Id");
        return v != null ? v : "unknown-device";
    }
    private String ip(HttpServletRequest r) {
        String h = r.getHeader("X-Forwarded-For");
        return (h != null && !h.isBlank()) ? h.split(",")[0].trim() : r.getRemoteAddr();
    }
    private String ua(HttpServletRequest r) {
        String s = r.getHeader("User-Agent");
        return s != null ? s : "";
    }
}
