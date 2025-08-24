package com.agri.mapapp.auth;

import java.time.Instant;

/** Auth natijasi — access/refresh va access expire vaqti */
public record TokenPair(
        String tokenType,          // "Bearer"
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt
) {}
