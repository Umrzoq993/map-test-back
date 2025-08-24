package com.agri.mapapp.auth;

import com.agri.mapapp.org.OrganizationUnit;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "ux_user_username", columnList = "username", unique = true),
        @Index(name = "ix_user_status", columnList = "status"),
        @Index(name = "ix_user_role", columnList = "role")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;

    // ---- Profil ----
    @Column(length = 160)
    private String fullName;      // F.I.O yagona maydon

    @Column(length = 120)
    private String position;      // Lavozim

    @Column(length = 120)
    private String title;         // Unvon

    @Column(length = 32)
    private String phone;         // Telefon

    @Column(length = 512)
    private String avatarUrl;     // Fotosurat URL

    // Tashkilot birligi (OrganizationUnit)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_unit_id")
    private OrganizationUnit orgUnit;

    @Column(length = 120)
    private String department;    // Bo'lim (oddiy satr)

    // Audit ustunlari
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = UserStatus.ACTIVE;
        if (role == null) role = Role.USER;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
