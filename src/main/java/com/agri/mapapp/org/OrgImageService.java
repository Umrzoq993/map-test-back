package com.agri.mapapp.org;

import com.agri.mapapp.common.ImageSniffer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrgImageService {
    private final OrgImageRepository repo;
    private final OrganizationUnitRepository orgRepo;

    private final SecureRandom rng = new SecureRandom();

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDirProp;

    private Path baseDir() {
        Path p = Paths.get(uploadsDirProp).toAbsolutePath().normalize();
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    public record OrgImageDto(Long id, String url, String originalName, String contentType, Long sizeBytes, Instant createdAt) {}

    @Transactional(readOnly = true)
    public List<OrgImageDto> list(Long orgId) {
        return repo.findByOrg_IdOrderByCreatedAtDesc(orgId).stream()
                .map(i -> new OrgImageDto(
                        i.getId(),
                        "/uploads/" + i.getFilename(),
                        i.getOriginalName(),
                        i.getContentType(),
                        i.getSizeBytes(),
                        i.getCreatedAt()
                )).toList();
    }

    @Transactional
    public OrgImageDto upload(Long orgId, MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");
        OrganizationUnit org = orgRepo.findById(orgId).orElseThrow(() -> new IllegalArgumentException("Org not found"));

        byte[] bytes = file.getBytes();
        if (bytes.length < 12) throw new IllegalArgumentException("Invalid image");
        ImageSniffer.Type type = ImageSniffer.sniff(bytes);
        if (type == null) throw new IllegalArgumentException("Unsupported image type");

        String safeOriginal = sanitizeOriginalName(file.getOriginalFilename());
        String generated = randomName() + type.getExt();
        // Use per-org subdir
        Path orgDir = baseDir().resolve("org").resolve(String.valueOf(orgId)).normalize();
        Files.createDirectories(orgDir);
        Path dest = orgDir.resolve(generated).normalize();
        // ensure inside basedir
        Path base = baseDir();
        if (!dest.toAbsolutePath().startsWith(base)) {
            throw new SecurityException("Invalid path");
        }
        // write atomically
        Path tmp = Files.createTempFile(orgDir, ".tmp-", type.getExt());
        Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        String rel = base.relativize(dest.toAbsolutePath()).toString().replace('\\','/');

        OrgImage saved = repo.save(OrgImage.builder()
                .org(org)
                .filename(rel)
                .originalName(safeOriginal)
                .contentType(type.getContentType())
                .sizeBytes((long) bytes.length)
                .createdByUserId(userId)
                .build());

        return new OrgImageDto(saved.getId(), "/uploads/" + saved.getFilename(), saved.getOriginalName(), saved.getContentType(), saved.getSizeBytes(), saved.getCreatedAt());
    }

    @Transactional
    public void delete(Long imageId) {
        OrgImage img = repo.findById(imageId).orElseThrow(() -> new IllegalArgumentException("Image not found"));
        Path base = baseDir();
        Path path = base.resolve(img.getFilename()).normalize();
        try {
            if (path.toAbsolutePath().startsWith(base) && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ignored) {}
        repo.delete(img);
    }

    private String sanitizeOriginalName(String originalFilename) {
        if (originalFilename == null) return null;
        String s = originalFilename.replaceAll("[\\r\\n\\t]", " ");
        if (s.length() > 200) s = s.substring(0, 200);
        return s;
    }

    private String randomName() {
        byte[] b = new byte[16];
        rng.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
