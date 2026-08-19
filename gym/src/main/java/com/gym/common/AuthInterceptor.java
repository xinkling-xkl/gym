package com.gym.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 角色鉴权拦截器：
 * - 把网关注入的 X-User-Account / X-User-Role / X-User-Name 解析到 UserContext；
 * - 无身份（服务间内部调用，如 login 查询用户、AI 管理员命令）信任放行；
 * - 会员（MEMBER）禁止越权访问管理员、员工、会员管理接口。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accountHeader = request.getHeader("X-User-Account");
        String role = request.getHeader("X-User-Role");
        String name = request.getHeader("X-User-Name");

        Integer account = null;
        if (accountHeader != null && !accountHeader.isEmpty()) {
            try {
                account = Integer.parseInt(accountHeader);
            } catch (NumberFormatException ignored) {
            }
        }

        if (account != null) {
            UserContext.set(new UserContext.UserInfo(account, name, role));
        }

        // 无身份：服务间内部调用，信任放行
        if (account == null) {
            return true;
        }

        // 仅会员需要做接口级限制（管理员/员工由业务层控制）
        if (!"MEMBER".equals(role)) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 会员禁止访问管理员模块
        if (path.startsWith("/api/admin/")) {
            return forbidden(response, "会员无权访问管理员接口");
        }

        // 会员访问员工模块：仅允许查教练列表、查本人
        if (path.startsWith("/api/employee/")) {
            boolean allowed = "GET".equals(method)
                    && (path.equals("/api/employee/coaches") || path.endsWith("/" + account));
            if (!allowed) {
                return forbidden(response, "会员无权操作员工信息");
            }
            return true;
        }

        // 会员访问会员模块：仅允许查本人、改本人资料、改本人密码
        if (path.startsWith("/api/member/")) {
            boolean allowed = ("GET".equals(method) && path.endsWith("/" + account))
                    || ("PUT".equals(method) && (path.equals("/api/member/profile") || path.equals("/api/member/password")));
            if (!allowed) {
                return forbidden(response, "会员无权执行该操作，卡级别等信息请到前台办理");
            }
            return true;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean forbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("code", 403);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
