package com.agri.mapapp.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "ix_refresh_token_token", columnList = "token", unique = true),
        @Index(name = "ix_refresh_token_user", columnList = "user_id"),
        @Index(name = "ix_refresh_token_user_device", columnList = "user_id,device_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200, unique = true)
    private String token; // random UUID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Column(name = "user_agent", length = 400)
    private String userAgent;

    @Column(name = "ip", length = 100)
    private String ip;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private String replacedByToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant lastSeenAt; // heartbeat yangilaydi

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (lastSeenAt == null) lastSeenAt = createdAt;
    }
}
