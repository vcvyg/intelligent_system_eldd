package org.example.persion.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.common.Result;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.ChatGroupAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated chat attachment upload endpoint.
 *
 * <p>Files are stored under generated UUID names. Browser-active formats such as
 * HTML, SVG and JavaScript are intentionally rejected.</p>
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FileUploadController {

    private static final long IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    private static final long AUDIO_MAX_BYTES = 20L * 1024 * 1024;
    private static final long FILE_MAX_BYTES = 20L * 1024 * 1024;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "m4a", "ogg", "webm", "aac");
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "pdf", "txt", "csv", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip"
    );

    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> AUDIO_MIME_TYPES = Set.of(
            "audio/wav", "audio/x-wav", "audio/mpeg", "audio/mp4", "audio/ogg",
            "audio/webm", "audio/aac", "audio/x-m4a"
    );
    private static final Set<String> FILE_MIME_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-zip-compressed"
    );

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    private final ChatGroupAccessService chatGroupAccessService;

    @PostMapping("/upload-audio")
    public Result<FileUploadResult> uploadAudio(@RequestParam("audio") MultipartFile file,
                                                @RequestParam("groupId") Long groupId) {
        assertGroupAccess(groupId);
        return store(file, AttachmentKind.AUDIO);
    }

    @PostMapping("/upload-image")
    public Result<FileUploadResult> uploadImage(@RequestParam("image") MultipartFile file,
                                                @RequestParam("groupId") Long groupId) {
        assertGroupAccess(groupId);
        return store(file, AttachmentKind.IMAGE);
    }

    /**
     * Generic authenticated attachment upload used by the current chat clients.
     * groupId is optional for backward compatibility; when supplied it is checked.
     */
    @PostMapping("/api/upload/file")
    public Result<FileUploadResult> uploadGeneralFile(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "groupId", required = false) Long groupId) {
        assertAuthenticated();
        assertOptionalGroupAccess(groupId);
        return store(file, inferKind(file));
    }

    @PostMapping("/api/upload/audio")
    public Result<FileUploadResult> uploadAudioFile(@RequestParam("audio") MultipartFile file,
                                                     @RequestParam(value = "groupId", required = false) Long groupId) {
        assertAuthenticated();
        assertOptionalGroupAccess(groupId);
        return store(file, AttachmentKind.AUDIO);
    }

    private Result<FileUploadResult> store(MultipartFile file, AttachmentKind kind) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = normalizeMimeType(file.getContentType());
        validate(kind, extension, mimeType, file.getSize());

        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path directory = root.resolve(kind.directory).resolve(date).normalize();
            if (!directory.startsWith(root)) {
                throw new BusinessException(400, "非法上传路径");
            }
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + "." + extension;
            Path destination = directory.resolve(filename).normalize();
            if (!destination.startsWith(directory)) {
                throw new BusinessException(400, "非法文件名");
            }
            Files.copy(file.getInputStream(), destination);

            String webPath = "/uploads/" + kind.directory + "/" + date + "/" + filename;
            FileUploadResult result = new FileUploadResult();
            result.setFilename(filename);
            result.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename()));
            result.setSize(file.getSize());
            result.setContentType(mimeType);
            result.setUrl(webPath);

            if (kind == AttachmentKind.IMAGE) {
                result.setImageUrl(webPath);
            } else if (kind == AttachmentKind.AUDIO) {
                result.setAudioUrl(webPath);
                result.setDuration(estimateAudioDuration(file.getSize()));
            } else {
                result.setFileUrl(webPath);
            }

            log.info("Stored chat attachment kind={}, size={} bytes", kind, file.getSize());
            return Result.success(result);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            log.error("Failed to store chat attachment", exception);
            throw new BusinessException(500, "文件上传失败，请稍后重试");
        }
    }

    private AttachmentKind inferKind(MultipartFile file) {
        String mimeType = normalizeMimeType(file.getContentType());
        if (mimeType.startsWith("image/")) {
            return AttachmentKind.IMAGE;
        }
        if (mimeType.startsWith("audio/")) {
            return AttachmentKind.AUDIO;
        }
        return AttachmentKind.FILE;
    }

    private void validate(AttachmentKind kind, String extension, String mimeType, long size) {
        Set<String> allowedExtensions;
        Set<String> allowedMimeTypes;
        long maxBytes;

        switch (kind) {
            case IMAGE -> {
                allowedExtensions = IMAGE_EXTENSIONS;
                allowedMimeTypes = IMAGE_MIME_TYPES;
                maxBytes = IMAGE_MAX_BYTES;
            }
            case AUDIO -> {
                allowedExtensions = AUDIO_EXTENSIONS;
                allowedMimeTypes = AUDIO_MIME_TYPES;
                maxBytes = AUDIO_MAX_BYTES;
            }
            case FILE -> {
                allowedExtensions = FILE_EXTENSIONS;
                allowedMimeTypes = FILE_MIME_TYPES;
                maxBytes = FILE_MAX_BYTES;
            }
            default -> throw new BusinessException(400, "不支持的文件类型");
        }

        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(400, "不支持的文件扩展名");
        }
        if (!allowedMimeTypes.contains(mimeType)) {
            throw new BusinessException(400, "不支持的文件类型");
        }
        if (size <= 0 || size > maxBytes) {
            throw new BusinessException(400, "文件大小超过允许范围");
        }
    }

    private void assertAuthenticated() {
        if (SecurityUtil.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private void assertGroupAccess(Long groupId) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!chatGroupAccessService.canAccess(userId, groupId)) {
            throw new BusinessException(403, "无权向该聊天群组上传附件");
        }
    }

    private void assertOptionalGroupAccess(Long groupId) {
        if (groupId != null) {
            assertGroupAccess(groupId);
        }
    }

    private String extensionOf(String originalFilename) {
        String safeName = safeOriginalFilename(originalFilename);
        int dot = safeName.lastIndexOf('.');
        if (dot < 0 || dot == safeName.length() - 1) {
            throw new BusinessException(400, "文件必须包含受支持的扩展名");
        }
        return safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String safeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String normalized = originalFilename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String filename = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return filename.replaceAll("[\\r\\n\\t]", "_");
    }

    private String normalizeMimeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).trim();
        int semicolon = normalized.indexOf(';');
        return semicolon >= 0 ? normalized.substring(0, semicolon).trim() : normalized;
    }

    private int estimateAudioDuration(long fileSize) {
        return (int) Math.min(Math.max(1, fileSize / 2048), 300);
    }

    private enum AttachmentKind {
        IMAGE("image"), AUDIO("audio"), FILE("file");

        private final String directory;

        AttachmentKind(String directory) {
            this.directory = directory;
        }
    }

    @Data
    public static class FileUploadResult {
        private String filename;
        private String originalFilename;
        private long size;
        private String contentType;
        private String audioUrl;
        private String imageUrl;
        private String url;
        private String fileUrl;
        private int duration;
    }
}
