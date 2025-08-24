package com.agri.mapapp.auth;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findByUser_IdAndDeviceIdAndRevokedFalse(Long userId, String deviceId);

    List<RefreshToken> findAllByRevokedFalse();

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.deviceId <> :deviceId and rt.revoked = false")
    int revokeAllOtherDevices(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("update RefreshToken rt set rt.lastSeenAt = :ts where rt.user.id = :userId and rt.deviceId = :deviceId and rt.revoked = false")
    int touchLastSeen(@Param("userId") Long userId, @Param("deviceId") String deviceId, @Param("ts") Instant ts);
}
