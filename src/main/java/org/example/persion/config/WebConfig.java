package org.example.persion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一使用外部路径: D:/intelligent_system/uploads
        
        String absoluteUploadPath = Paths.get(uploadPath).toAbsolutePath().toString();
        String externalPath = "file:" + absoluteUploadPath.replace("\\\\", "/") + "/";

        System.out.println("配置uploads资源访问路径:");
        System.out.println("  - 统一路径: " + externalPath);
        System.out.println("  - 绝对路径: " + absoluteUploadPath);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(externalPath)
                .setCachePeriod(0); // 开发时禁用缓存

        // 默认的静态资源 (css, js, html等) 仍然从classpath加载
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}