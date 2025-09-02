package com.agri.mapapp.org;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
@Deprecated
public class OrgImageController {

    @GetMapping("/{orgId}/images")
    public ResponseEntity<List<?>> list(Authentication auth, @PathVariable Long orgId) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @PostMapping("/{orgId}/images")
    public ResponseEntity<?> upload(Authentication auth, @PathVariable Long orgId) {
        return ResponseEntity.status(HttpStatus.GONE).body("Org images deprecated. Use /api/facilities/{facilityId}/images");
    }

    @DeleteMapping("/{orgId}/images/{imageId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long orgId, @PathVariable Long imageId) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
