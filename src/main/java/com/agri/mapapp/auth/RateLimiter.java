package com.agri.mapapp.auth;

public interface RateLimiter {
    boolean tryConsumeLogin(String key);
    boolean tryConsumeRefresh(String key);
}
