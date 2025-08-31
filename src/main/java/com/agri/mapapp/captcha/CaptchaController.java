package com.agri.mapapp.captcha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/captcha")
@Validated
public class CaptchaController {

    private final CaptchaService service;

    public CaptchaController(CaptchaService service) {
        this.service = service;
    }

    public record NewCaptchaRes(String id, String image, long ttlSeconds) {}
    public record VerifyReq(@NotBlank String id, @NotBlank String answer) {}

    @GetMapping("/new")
    public ResponseEntity<?> newCaptcha() {
        if (!service.isEnabled()) {
            Map<String, Object> body = new HashMap<>();
            body.put("enabled", false);
            return ResponseEntity.ok(body);
        }
        CaptchaService.Captcha c = service.createCaptcha();
        return ResponseEntity.ok(new NewCaptchaRes(c.id(), c.imageBase64(), service.getTtlSeconds()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyReq req) {
        boolean ok = service.verifyAndConsume(req.id(), req.answer());
        Map<String, Object> body = new HashMap<>();
        body.put("ok", ok);
        return ResponseEntity.ok(body);
    }
}
