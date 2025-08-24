package com.agri.mapapp.audit.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRes {
    private Long id;
    private String event;
    private Long userId;
    private String username;
    private String deviceId;
    private String ip;
    private String userAgent;
    private Instant ts;
}
