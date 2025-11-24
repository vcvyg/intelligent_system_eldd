package org.example.persion.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables MyBatis-Plus pagination support (required for selectPage).
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.SQL_SERVER);
        pagination.setOverflow(true); // 回到第一页防止空数据
        pagination.setMaxLimit(1000L);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}

