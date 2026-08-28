package org.example.persion.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves files that were created under the configured upload root.
 * Public download is retained for the current browser chat implementation, so
 * path confinement is enforced here rather than relying on caller identity.
 */
@Slf4j
@RestController
public class FileDownloadController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String filePath) {
        try {
            Path actualPath = resolveUploadWebPath(filePath);
            if (actualPath == null) {
                return ResponseEntity.badRequest().build();
            }
            if (!Files.isRegularFile(actualPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(actualPath);
            String filename = actualPath.getFileName().toString();
            String contentType = safeContentType(actualPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (IOException exception) {
            log.warn("Attachment download failed: {}", exception.getClass().getSimpleName());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/file-access/uploads/**")
    public ResponseEntity<Resource> serveFile(
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            HttpServletRequest request) {
        try {
            String requestUri = request.getRequestURI();
            int index = requestUri.indexOf("/uploads/");
            if (index < 0) {
                return ResponseEntity.badRequest().build();
            }

            Path actualPath = resolveUploadWebPath(requestUri.substring(index));
            if (actualPath == null) {
                return ResponseEntity.badRequest().build();
            }
            if (!Files.isRegularFile(actualPath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = safeContentType(actualPath);
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("X-Content-Type-Options", "nosniff");

            if (download) {
                String filename = actualPath.getFileName().toString();
                builder.header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            }
            return builder.body(new FileSystemResource(actualPath));
        } catch (IOException exception) {
            log.warn("Attachment access failed: {}", exception.getClass().getSimpleName());
            return ResponseEntity.notFound().build();
        }
    }

    private Path resolveUploadWebPath(String webPath) throws IOException {
        if (webPath == null || !webPath.startsWith("/uploads/")) {
            return null;
        }

        String relative = webPath.substring("/uploads/".length());
        if (relative.isBlank() || relative.indexOf('\0') >= 0) {
            return null;
        }

        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) {
            return null;
        }
        if (!Files.exists(candidate)) {
            return candidate;
        }

        // Resolve symlinks only after the lexical confinement check, then enforce
        // confinement again against the real upload root.
        Path realRoot = Files.exists(root) ? root.toRealPath() : root;
        Path realCandidate = candidate.toRealPath();
        return realCandidate.startsWith(realRoot) ? realCandidate : null;
    }

    private String safeContentType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        return contentType == null ? "application/octet-stream" : contentType;
    }
}
