package com.agri.mapapp.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "ix_refresh_token_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "ix_refresh_token_user", columnList = "user_id"),
        @Index(name = "ix_refresh_token_user_device", columnList = "user_id,device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Legacy raw token (no longer used). Keep nullable for backward-compat */
    @Column(name = "token", length = 64)
    private String token;

    /** Hash of refresh token value (e.g., sha256(salt+token)) */
    @Column(name = "token_hash", unique = true, length = 128)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    /** Rotation paytida qaysi token o‘rniga chiqqani (audit uchun, hash qiymati) */
    @Column(name = "replaced_by_token_hash", length = 128)
    private String replacedByTokenHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Client heartbeat yoki har bir refresh so‘rovda yangilanadi */
    @Column
    private Instant lastSeenAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (lastSeenAt == null) lastSeenAt = now;
        if (!revoked && expiresAt != null && expiresAt.isBefore(now)) {
            revoked = true; // ehtiyot chorasi
        }
    }
}
