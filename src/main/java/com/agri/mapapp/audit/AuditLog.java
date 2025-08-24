package com.agri.mapapp.audit;

import com.agri.mapapp.auth.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "ix_audit_ts", columnList = "ts"),
        @Index(name = "ix_audit_user", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LOGIN_SUCCESS, REFRESH_ROTATE, REFRESH_REUSE, LOGOUT, SESSION_REVOKE, SESSION_REJECTED */
    @Column(nullable = false, length = 40)
    private String event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(length = 64)
    private String deviceId;

    @Column(length = 64)
    private String ip;

    @Column(length = 256)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant ts;

    @PrePersist
    public void prePersist() {
        if (ts == null) ts = Instant.now();
    }
}
