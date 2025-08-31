package com.agri.mapapp.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class CaptchaService {

    private final CaptchaStore store;

    @Value("${app.captcha.enabled:true}")
    private boolean enabled;
    @Value("${app.captcha.length:5}")
    private int length;
    @Value("${app.captcha.ttl-seconds:120}")
    private long ttlSeconds;
    @Value("${app.captcha.width:160}")
    private int width;
    @Value("${app.captcha.height:60}")
    private int height;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // exclude 0,O,1,I
    private static final SecureRandom RNG = new SecureRandom();

    public CaptchaService(CaptchaStore store) {
        this.store = store;
    }

    public record Captcha(String id, String answer, String imageBase64) {}

    public boolean isEnabled() { return enabled; }
    public long getTtlSeconds() { return ttlSeconds; }

    public Captcha createCaptcha() {
        if (!enabled) {
            throw new IllegalStateException("Captcha disabled");
        }
        String id = UUID.randomUUID().toString();
        String answer = randomText(length);
        String dataUri = generateImageDataUri(answer);
        store.save("captcha:" + id, answer, Duration.ofSeconds(ttlSeconds));
        return new Captcha(id, answer, dataUri);
    }

    public boolean verifyAndConsume(String id, String userInput) {
        if (!enabled) return true; // if disabled, always pass
        if (id == null || id.isBlank()) return false;
        String key = "captcha:" + id;
        String expected = store.get(key);
        if (expected == null) return false;
        store.delete(key);
        return expected.equalsIgnoreCase(userInput == null ? "" : userInput.trim());
    }

    private String randomText(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int idx = RNG.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    private String generateImageDataUri(String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            // background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // light noise background
            for (int i = 0; i < 20; i++) {
                g.setColor(new Color(220 + RNG.nextInt(35), 220 + RNG.nextInt(35), 220 + RNG.nextInt(35)));
                int x1 = RNG.nextInt(width), y1 = RNG.nextInt(height);
                int x2 = RNG.nextInt(width), y2 = RNG.nextInt(height);
                g.drawLine(x1, y1, x2, y2);
            }

            // text
            int fontSize = (int) (height * 0.6);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();

            int charWidth = width / (text.length() + 1);
            int baseY = (height - fm.getHeight()) / 2 + fm.getAscent();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                // vary color slightly
                g.setColor(new Color(20 + RNG.nextInt(120), 20 + RNG.nextInt(120), 20 + RNG.nextInt(120)));
                // slight rotation per char
                double angle = Math.toRadians(RNG.nextInt(21) - 10);
                AffineTransform old = g.getTransform();
                int x = (i + 1) * charWidth - fm.charWidth(c) / 2;
                g.rotate(angle, x, baseY);
                g.drawString(String.valueOf(c), x, baseY);
                g.setTransform(old);
            }

            // foreground noise dots
            for (int i = 0; i < width; i += 4) {
                g.setColor(new Color(RNG.nextInt(200), RNG.nextInt(200), RNG.nextInt(200)));
                int x = RNG.nextInt(width);
                int y = RNG.nextInt(height);
                g.fillRect(x, y, 1, 1);
            }
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render captcha", e);
        }
    }
}
