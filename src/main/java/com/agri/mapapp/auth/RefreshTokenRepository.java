// src/main/java/com/agri/mapapp/auth/RefreshTokenRepository.java
package com.agri.mapapp.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Hashed lookups (new)
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Legacy raw token lookups (for migration)
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser_Id(Long userId);

    List<RefreshToken> findAllByUser_IdAndRevokedFalse(Long userId);

    List<RefreshToken> findAllByUser_IdAndRevokedFalseOrderByCreatedAtAsc(Long userId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update RefreshToken r set r.lastSeenAt = :ts where r.user.id = :userId and r.deviceId = :deviceId")
    void touchLastSeen(@Param("userId") Long userId, @Param("deviceId") String deviceId, @Param("ts") Instant ts);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.deviceId <> :keepDeviceId")
    void revokeAllOtherDevices(@Param("userId") Long userId, @Param("keepDeviceId") String keepDeviceId);

    /** ✅ Paging + filtrlash (revoked/expired) */
    @Query("""
           select r from RefreshToken r
           where r.user.id = :userId
             and (:includeRevoked = true or r.revoked = false)
             and (:includeExpired = true or r.expiresAt > :now)
           """)
    Page<RefreshToken> findSessions(@Param("userId") Long userId,
                                    @Param("includeRevoked") boolean includeRevoked,
                                    @Param("includeExpired") boolean includeExpired,
                                    @Param("now") Instant now,
                                    Pageable pageable);

    Optional<RefreshToken> findFirstByUser_IdAndDeviceIdOrderByCreatedAtDesc(Long userId, String deviceId);
}
