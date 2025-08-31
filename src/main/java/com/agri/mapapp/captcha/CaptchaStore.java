package com.agri.mapapp.captcha;

import java.time.Duration;

public interface CaptchaStore {
    void save(String key, String value, Duration ttl);
    String get(String key);
    void delete(String key);
}

