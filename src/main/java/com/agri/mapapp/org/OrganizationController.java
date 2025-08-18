package com.agri.mapapp.org;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/org")
public class OrganizationController {

    private final OrganizationService service;

    @GetMapping("/tree")
    public ResponseEntity<List<OrgNodeDto>> tree() {
        return ResponseEntity.ok(service.getOrgTree());
    }
}

