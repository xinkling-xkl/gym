package org.example.plan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 统一时间服务
 * 通过 Nacos common.yaml 的 mock.time 配置可模拟任意时间，方便测试与时间相关的业务逻辑
 * 配置格式：mock.time: 2026-12-31T10:00:00 （ISO LocalDateTime 格式）
 * 留空或不配置则使用真实系统时间
 * 修改配置后通过 @RefreshScope 热刷新或重启服务即可生效
 */
@Service
@RefreshScope
public class TimeService {

    @Value("${mock.time:}")
    private String mockTime;

    public LocalDateTime nowDateTime() {
        if (mockTime != null && !mockTime.isEmpty()) {
            return LocalDateTime.parse(mockTime);
        }
        return LocalDateTime.now();
    }

    public LocalDate nowDate() {
        if (mockTime != null && !mockTime.isEmpty()) {
            return LocalDate.parse(mockTime.substring(0, Math.min(10, mockTime.length())));
        }
        return LocalDate.now();
    }

    public Date legacyDate() {
        if (mockTime != null && !mockTime.isEmpty()) {
            return Date.from(LocalDateTime.parse(mockTime)
                    .atZone(ZoneId.systemDefault()).toInstant());
        }
        return new Date();
    }

    public Clock clock() {
        if (mockTime != null && !mockTime.isEmpty()) {
            return Clock.fixed(
                    LocalDateTime.parse(mockTime).atZone(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault()
            );
        }
        return Clock.systemDefaultZone();
    }
}
