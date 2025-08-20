package com.agri.mapapp.auth;

import com.agri.mapapp.org.OrganizationUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true, nullable=false, length=100)
    private String username;

    @Column(nullable=false) // BCrypt
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private Role role;

    // Foydalanuvchi “bazaviy” org’iga biriktiriladi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private OrganizationUnit org; // null bo‘lishi ham mumkin (ADMINlar uchun ixtiyoriy)
}
