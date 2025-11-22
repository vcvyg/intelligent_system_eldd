package org.example.persion.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FileUploadController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 上传音频文件
     */
    @PostMapping("/upload-audio")
    public Result<FileUploadResult> uploadAudio(@RequestParam("audio") MultipartFile file,
                                               @RequestParam("groupId") Long groupId,
                                               HttpServletRequest request) {
        return uploadFile(file, "audio", groupId, request);
    }

    /**
     * 上传图片文件
     */
    @PostMapping("/upload-image")
    public Result<FileUploadResult> uploadImage(@RequestParam("image") MultipartFile file,
                                               @RequestParam("groupId") Long groupId,
                                               HttpServletRequest request) {
        return uploadFile(file, "image", groupId, request);
    }

    /**
     * 通用文件上传 - 用于子女端聊天
     */
    @PostMapping("/api/upload/file")
    public Result<FileUploadResult> uploadGeneralFile(@RequestParam("file") MultipartFile file,
                                                     HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            String contentType = file.getContentType();
            String type = "file"; // 默认类型
            String messageType = "FILE"; // 默认消息类型
            
            // 根据文件类型确定存储目录和消息类型
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    type = "image";
                    messageType = "IMAGE";
                } else if (contentType.startsWith("audio/")) {
                    type = "audio";
                    messageType = "VOICE";
                } else {
                    type = "file";
                    messageType = "FILE";
                }
            }

            // 创建上传目录
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path uploadDirPath = Paths.get(uploadPath, type, dateStr);

            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDirPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // 生成Web访问路径
            String webPath = "/uploads/" + type + "/" + dateStr + "/" + filename;

            FileUploadResult result = new FileUploadResult();
            result.setFilename(filename);
            result.setOriginalFilename(originalFilename);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setUrl(webPath); // 通用URL字段
            result.setFileName(originalFilename); // 兼容前端字段名
            
            // 根据文件类型设置对应的URL字段
            if ("IMAGE".equals(messageType)) {
                result.setImageUrl(webPath);
            } else if ("VOICE".equals(messageType)) {
                result.setAudioUrl(webPath);
                result.setDuration(estimateAudioDuration(file.getSize()));
            } else {
                result.setFileUrl(webPath);
            }

            return Result.success(result);

        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 音频上传 - 用于子女端聊天
     */
    @PostMapping("/api/upload/audio")
    public Result<FileUploadResult> uploadAudioFile(@RequestParam("audio") MultipartFile file,
                                                   HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("音频文件不能为空");
        }

        try {
            // 创建上传目录
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path uploadDirPath = Paths.get(uploadPath, "audio", dateStr);

            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDirPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // 生成Web访问路径
            String webPath = "/uploads/audio/" + dateStr + "/" + filename;

            FileUploadResult result = new FileUploadResult();
            result.setFilename(filename);
            result.setOriginalFilename(originalFilename);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setUrl(webPath); // 通用URL字段
            result.setAudioUrl(webPath); // 音频专用URL
            result.setDuration(estimateAudioDuration(file.getSize()));

            return Result.success(result);

        } catch (IOException e) {
            return Result.error("音频上传失败: " + e.getMessage());
        }
    }

    private Result<FileUploadResult> uploadFile(MultipartFile file, String type, Long groupId, HttpServletRequest request) {
        System.out.println("开始上传文件 - 类型: " + type + ", 群组ID: " + groupId + ", 文件大小: " + file.getSize());
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            // 创建上传目录 - 使用配置的路径
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path uploadDirPath = Paths.get(uploadPath, type, dateStr);

            System.out.println("上传目录: " + uploadDirPath.toAbsolutePath());

            if (!Files.exists(uploadDirPath)) {
                Files.createDirectories(uploadDirPath);
                System.out.println("创建目录: " + uploadDirPath.toAbsolutePath());
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDirPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            System.out.println("文件保存到: " + filePath.toAbsolutePath());

            // 生成Web访问路径
            String webPath = "/uploads/" + type + "/" + dateStr + "/" + filename;
            System.out.println("文件Web访问路径: " + webPath);

            FileUploadResult result = new FileUploadResult();
            result.setFilename(filename);
            result.setOriginalFilename(originalFilename);
            result.setSize(file.getSize());
            result.setContentType(file.getContentType());
            
            if ("audio".equals(type)) {
                result.setAudioUrl(webPath); // 直接使用Web路径
                result.setDuration(estimateAudioDuration(file.getSize()));
                System.out.println("音频文件 - Web路径: " + webPath + ", 时长: " + result.getDuration() + "秒");
            } else if ("image".equals(type)) {
                result.setImageUrl(webPath); // 直接使用Web路径
                System.out.println("图片文件 - Web路径: " + webPath);
            }

            System.out.println("文件上传成功: " + result);
            return Result.success(result);

        } catch (IOException e) {
            System.err.println("文件上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 简单估算音频时长（基于文件大小）
     * 实际项目中应该使用音频处理库来获取准确时长
     */
    private int estimateAudioDuration(long fileSize) {
        System.out.println("估算音频时长 - 文件大小: " + fileSize + " bytes");
        
        if (fileSize < 1024) {
            System.out.println("文件太小，设置为1秒");
            return 1; // 最小1秒
        }
        
        // 对于浏览器录制的音频，通常压缩比较高
        // 经验值：大约每秒2-3KB的压缩音频
        // 使用保守估算：每2KB约1秒
        int estimatedSeconds = (int) Math.max(1, fileSize / 2048);
        
        // 限制最大时长为300秒（5分钟）
        int finalDuration = Math.min(estimatedSeconds, 300);
        
        System.out.println("估算结果: " + finalDuration + " 秒 (基于 " + fileSize + " bytes)");
        return finalDuration;
    }

    public static class FileUploadResult {
        private String filename;
        private String originalFilename;
        private long size;
        private String contentType;
        private String audioUrl;
        private String imageUrl;
        private String url; // 通用URL字段
        private String fileUrl; // 文件URL字段
        private String fileName; // 兼容前端字段名
        private int duration; // 音频时长（秒）
        
        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
}