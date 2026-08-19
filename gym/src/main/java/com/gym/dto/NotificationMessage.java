package com.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跨服务通知消息体（gym 生产者 -> RocketMQ -> notification-service 消费者）
 * 字段须与 notification-service 的 NotificationMessage 保持一致，保证 JSON 序列化互通。
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
