package org.example.login.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_KEY = "gym-management-jwt-secret-key-2024";
    private static final long EXPIRATION_TIME = 86400000;

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

    public static String generateToken(Integer account, String name, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("name", name);
        claims.put("role", role);

        long nowMillis = clock.millis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(account))
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setClock(() -> new Date(clock.millis()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Integer getAccountFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("account", Integer.class);
    }

    public static String getNameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("name", String.class);
    }

    public static String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date(clock.millis()));
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
