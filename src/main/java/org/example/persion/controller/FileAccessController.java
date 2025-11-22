package org.example.persion.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/api/files")
public class FileAccessController {

    /**
     * 提供音频文件访问
     */
    @GetMapping("/audio/{filename}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String filename, 
                                                @RequestParam String path) {
        return getFile(path, "audio");
    }

    /**
     * 提供图片文件访问
     */
    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImageFile(@PathVariable String filename, 
                                                @RequestParam String path) {
        return getFile(path, "image");
    }

    private ResponseEntity<Resource> getFile(String encodedPath, String type) {
        try {
            // 解码文件路径
            String filePath = new String(Base64.getDecoder().decode(encodedPath));
            System.out.println("访问文件: " + filePath);
            
            Path path = Paths.get(filePath);
            
            // 检查文件是否存在
            if (!Files.exists(path)) {
                System.err.println("文件不存在: " + filePath);
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否是文件
            if (!Files.isRegularFile(path)) {
                System.err.println("不是有效文件: " + filePath);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(path);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            
            // 根据文件类型设置Content-Type
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                if ("audio".equals(type)) {
                    contentType = "audio/wav";
                } else if ("image".equals(type)) {
                    contentType = "image/jpeg";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(Files.size(path));
            
            // 设置缓存控制
            headers.setCacheControl("max-age=3600"); // 缓存1小时
            
            System.out.println("成功提供文件访问: " + filePath + ", Content-Type: " + contentType);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("文件访问失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}