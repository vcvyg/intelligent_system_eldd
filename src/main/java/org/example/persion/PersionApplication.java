package org.example.persion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智慧养老系统启动类
 */
@SpringBootApplication
@MapperScan("org.example.persion.repository")
@EnableScheduling
@EnableAsync  // 启用异步支持
public class PersionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersionApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("智慧养老系统启动成功!");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("========================================\n");
    }

}
