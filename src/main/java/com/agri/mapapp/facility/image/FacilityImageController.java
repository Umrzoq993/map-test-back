package com.agri.mapapp.facility.image;

import com.agri.mapapp.auth.Role;
import com.agri.mapapp.auth.UserPrincipal;
import com.agri.mapapp.facility.FacilityRepository;
import com.agri.mapapp.org.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityImageController {

    private final FacilityImageService service;
    private final FacilityImageRepository imageRepo;
    private final FacilityRepository facilityRepo;
    private final AccessService accessService;

    private boolean canAccessFacility(Authentication auth, Long facilityId) {
        var f = facilityRepo.findById(facilityId).orElse(null);
        if (f == null) return false;
        var up = (UserPrincipal) auth.getPrincipal();
        if (up.getRole() == Role.ADMIN) return true;
        return accessService.canAccessOrg(auth, f.getOrg().getId());
    }

    @GetMapping("/{facilityId}/images")
    public ResponseEntity<List<FacilityImageService.FacilityImageDto>> list(Authentication auth, @PathVariable Long facilityId) {
        if (!canAccessFacility(auth, facilityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.list(facilityId));
    }

    @PostMapping(value = "/{facilityId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(Authentication auth,
                                    @PathVariable Long facilityId,
                                    @RequestPart("file") MultipartFile file) {
        try {
            if (!canAccessFacility(auth, facilityId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            UserPrincipal up = (UserPrincipal) auth.getPrincipal();
            var dto = service.upload(facilityId, file, up.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save image");
        }
    }

    @DeleteMapping("/{facilityId}/images/{imageId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long facilityId, @PathVariable Long imageId) {
        if (!canAccessFacility(auth, facilityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var img = imageRepo.findById(imageId);
        if (img.isPresent()) {
            if (!img.get().getFacility().getId().equals(facilityId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            service.delete(imageId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

