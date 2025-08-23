package com.agri.mapapp.org;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_unit")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationUnit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private OrganizationUnit parent; // null => root

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "code", length = 32, unique = true, nullable = false)
    private String code;

    private Double lat;   // null bo‘lishi mumkin
    private Double lng;
    private Integer zoom; // ixtiyoriy

    private Integer sortOrder; // ixtiyoriy tartib
}
