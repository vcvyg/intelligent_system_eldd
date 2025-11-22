package org.example.persion.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件下载控制器 - 完全开放的文件访问，无需任何认证
 */
@RestController
public class FileDownloadController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * 下载文件 - 完全开放，无需任何认证
     * 路径格式: /download?path=/uploads/file/2024-01-01/filename.ext
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String filePath) {
        try {
            System.out.println("文件下载请求 - 路径: " + filePath);
            
            // 安全检查：确保路径在uploads目录下
            if (!filePath.startsWith("/uploads/")) {
                System.err.println("非法文件路径: " + filePath);
                return ResponseEntity.badRequest().build();
            }
            
            // 移除开头的斜杠，构建实际文件路径
            String relativePath = filePath.substring(1); // 移除开头的 "/"
            Path actualPath = Paths.get(uploadPath).resolve(relativePath.substring("uploads/".length()));
            
            System.out.println("实际文件路径: " + actualPath.toAbsolutePath());
            
            File file = actualPath.toFile();
            if (!file.exists() || !file.isFile()) {
                System.err.println("文件不存在: " + actualPath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 创建资源
            Resource resource = new FileSystemResource(file);
            
            // 获取文件名和MIME类型
            String filename = file.getName();
            String contentType = Files.probeContentType(actualPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            System.out.println("下载文件: " + filename + ", 类型: " + contentType + ", 大小: " + file.length());
            
            // 设置响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .body(resource);
                    
        } catch (IOException e) {
            System.err.println("文件下载失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 直接通过路径访问文件 - 完全开放访问
     * 路径格式: /file-access/uploads/file/2024-01-01/filename.ext
     */
    @GetMapping("/file-access/uploads/**")
    public ResponseEntity<Resource> serveFile(@RequestParam(value = "download", defaultValue = "false") boolean download) {
        try {
            // 从请求URI中提取文件路径
            String requestURI = ((jakarta.servlet.http.HttpServletRequest) 
                org.springframework.web.context.request.RequestContextHolder
                    .currentRequestAttributes()
                    .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST))
                    .getRequestURI();
            
            System.out.println("文件访问请求 - URI: " + requestURI);
            
            // 提取uploads后的路径
            String uploadsPath = requestURI.substring(requestURI.indexOf("/uploads/") + "/uploads/".length());
            Path actualPath = Paths.get(uploadPath, uploadsPath);
            
            System.out.println("实际文件路径: " + actualPath.toAbsolutePath());
            
            File file = actualPath.toFile();
            if (!file.exists() || !file.isFile()) {
                System.err.println("文件不存在: " + actualPath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String filename = file.getName();
            String contentType = Files.probeContentType(actualPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            System.out.println("访问文件: " + filename + ", 类型: " + contentType);
            
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            
            // 如果是下载请求，添加下载头
            if (download) {
                builder.header(HttpHeaders.CONTENT_DISPOSITION, 
                              "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            }
            
            return builder.body(resource);
            
        } catch (Exception e) {
            System.err.println("文件访问失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}