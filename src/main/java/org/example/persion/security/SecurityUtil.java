package org.example.persion.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类，用于获取当前用户信息
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户的ID
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
}
