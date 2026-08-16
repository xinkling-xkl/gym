package com.xk.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY = "gym-management-jwt-secret-key-2024";

    /**
     * 可替换的时钟，由 TimeService 在启动和配置刷新时注入
     * 默认使用系统真实时钟，配置 mock.time 后会切换为固定时钟
     */
    private static volatile Clock clock = Clock.systemDefaultZone();

    public static void setClock(Clock newClock) {
        clock = newClock;
    }

    private static SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setClock(() -> new Date(clock.millis()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
