package com.agri.mapapp.facility.image;

import com.agri.mapapp.facility.Facility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "facility_image", indexes = {
        @Index(name = "idx_facility_image_facility", columnList = "facility_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacilityImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false, length = 255)
    private String filename;          // stored relative path inside uploads dir

    @Column(length = 255)
    private String originalName;      // sanitized display name

    @Column(length = 100)
    private String contentType;

    private Long sizeBytes;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Long createdByUserId;
}

