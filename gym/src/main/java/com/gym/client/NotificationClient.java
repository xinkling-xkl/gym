package com.gym.client;

import com.gym.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 调用 notification-server 发送通知的 Feign 客户端
 */
@FeignClient(name = "notification-server", contextId = "notificationClient")
public interface NotificationClient {

    /**
     * 发送通知
     * body 字段：userAccount, title, content, type
     */
    @PostMapping("/api/notification/send")
    Result<Void> sendNotification(@RequestBody Map<String, Object> body);
}
