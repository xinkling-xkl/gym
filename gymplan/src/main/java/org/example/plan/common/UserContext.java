package org.example.plan.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具：从网关注入的请求头中获取当前登录用户信息
 * 网关 AuthGlobalFilter 解析 JWT 后注入 X-User-Account / X-User-Name / X-User-Role
 */
public class UserContext {

    public static String getCurrentAccount() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        return request.getHeader("X-User-Account");
    }

    public static String getCurrentRole() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        return request.getHeader("X-User-Role");
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
