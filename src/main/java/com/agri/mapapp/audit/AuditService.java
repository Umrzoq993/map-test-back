package com.agri.mapapp.audit;

import com.agri.mapapp.auth.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void log(String event, AppUser user, String deviceId, String ip, String ua) {
        repo.save(AuditLog.builder()
                .event(event)
                .user(user)
                .deviceId(deviceId)
                .ip(ip)
                .userAgent(ua)
                .build());
    }
}
