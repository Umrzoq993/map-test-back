package com.agri.mapapp.facility;

import com.agri.mapapp.org.OrganizationUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "facility",
        indexes = {
                @Index(name = "idx_facility_org", columnList = "org_id"),
                @Index(name = "idx_facility_type", columnList = "type"),
                @Index(name = "idx_facility_lat_lng", columnList = "lat,lng")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private OrganizationUnit org;     // tegishli bo'linma

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityStatus status;

    // joylashuv
    private Double lat;
    private Double lng;

    // ixtiyoriy zoom (markerga uchishda ishlatsa bo'ladi)
    private Integer zoom;

    // qo'shimcha ma'lumotlar (moslashuvchan)
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode attributes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = FacilityStatus.ACTIVE;
    }
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
