package com.xk.gateway.filter;

import com.xk.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(
            "/api/login",
            "/api/login/validate",
            "/pictures",
            "/ws"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        for (String white : WHITE_LIST) {
            if (path.startsWith(white)) {
                return chain.filter(exchange);
            }
        }

        // 提取 Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供有效的认证令牌");
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.validateToken(token)) {
            return unauthorized(exchange, "令牌无效或已过期");
        }

        // 解析 JWT 将用户信息注入 Header，下游服务直接读取
        var claims = JwtUtil.parseToken(token);
        exchange = exchange.mutate()
                .request(r -> {
                    // 先移除客户端可能伪造的同名头，防止角色越权注入
                    r.headers(h -> {
                        h.remove("X-User-Account");
                        h.remove("X-User-Name");
                        h.remove("X-User-Role");
                    });
                    r.header("X-User-Account", String.valueOf(claims.get("account")))
                            .header("X-User-Name", String.valueOf(claims.get("name")))
                            .header("X-User-Role", String.valueOf(claims.get("role")));
                })
                .build();

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
