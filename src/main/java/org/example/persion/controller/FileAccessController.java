package org.example.persion.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Legacy authenticated file-access API kept for compatibility.
 * Decoded paths are strictly confined to the configured upload directory.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileAccessController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/audio/{filename}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String filename,
                                                 @RequestParam String path) {
        return getFile(path, "audio/");
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImageFile(@PathVariable String filename,
                                                 @RequestParam String path) {
        return getFile(path, "image/");
    }

    private ResponseEntity<Resource> getFile(String encodedPath, String expectedMimePrefix) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encodedPath), StandardCharsets.UTF_8);
            Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path supplied = Paths.get(decoded);
            Path candidate = supplied.isAbsolute()
                    ? supplied.toAbsolutePath().normalize()
                    : root.resolve(supplied).normalize();

            if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
                return ResponseEntity.notFound().build();
            }

            Path realRoot = Files.exists(root) ? root.toRealPath() : root;
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realRoot)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(realCandidate);
            if (contentType == null || !contentType.toLowerCase().startsWith(expectedMimePrefix)) {
                return ResponseEntity.badRequest().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(Files.size(realCandidate));
            headers.setCacheControl("private, max-age=3600");
            headers.set("X-Content-Type-Options", "nosniff");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new FileSystemResource(realCandidate));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (Exception exception) {
            log.warn("Legacy file access failed: {}", exception.getClass().getSimpleName());
            return ResponseEntity.notFound().build();
        }
    }
}
