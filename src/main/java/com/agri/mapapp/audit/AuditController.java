package com.agri.mapapp.audit;

import com.agri.mapapp.audit.dto.AuditRes;
import com.agri.mapapp.common.PageResponse;
import com.agri.mapapp.auth.AppUser;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogRepository repo;

    @GetMapping
    public ResponseEntity<PageResponse<AuditRes>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "ts,desc") String sort,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Specification<AuditLog> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (deviceId != null && !deviceId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("deviceId"), deviceId));
        }
        if (event != null && !event.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("event"), event));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("ts"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("ts"), to));
        }

        Page<AuditLog> pg = repo.findAll(spec, pageable);

        List<AuditRes> content = new ArrayList<>(pg.getNumberOfElements());
        for (AuditLog a : pg.getContent()) {
            AppUser u = a.getUser();
            content.add(AuditRes.builder()
                    .id(a.getId())
                    .event(a.getEvent())
                    .userId(u != null ? u.getId() : null)
                    .username(u != null ? u.getUsername() : null)
                    .deviceId(a.getDeviceId())
                    .ip(a.getIp())
                    .userAgent(a.getUserAgent())
                    .ts(a.getTs())
                    .build());
        }

        // Sizdagi PageResponse konstruktoriga mos: (content, page, size, totalElements, totalPages, last)
        PageResponse<AuditRes> resp = new PageResponse<>(
                content,
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.isLast()
        );

        return ResponseEntity.ok(resp);
    }

    private Sort parseSort(String sort) {
        // format: "field,dir" yoki "field" (default: ts desc)
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("ts"));
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";
        if (dir.startsWith("asc")) return Sort.by(Sort.Order.asc(field));
        return Sort.by(Sort.Order.desc(field));
    }
}
