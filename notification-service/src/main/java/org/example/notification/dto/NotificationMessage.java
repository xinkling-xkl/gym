package org.example.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跨服务通知消息体（与 gym 的 NotificationMessage 字段保持一致，保证 JSON 序列化互通）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    private String userAccount;
    private String title;
    private String content;
    private String type;
}
