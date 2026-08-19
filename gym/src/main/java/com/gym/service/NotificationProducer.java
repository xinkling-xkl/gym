package com.gym.service;

import com.gym.client.NotificationClient;
import com.gym.dto.NotificationMessage;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知消息生产者：优先走 RocketMQ 异步投递，MQ 不可用时降级 Feign 同步调用。
 */
@Component
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);
    public static final String TOPIC = "notify-topic";

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    private NotificationClient notificationClient;

    /**
     * 兼容历史调用方式：Map body -> NotificationMessage -> MQ
     * 字段：userAccount, title, content, type
     */
    public void sendNotification(Map<String, Object> body) {
        if (body == null) {
            return;
        }
        NotificationMessage msg = new NotificationMessage(
                asString(body.get("userAccount")),
                asString(body.get("title")),
                asString(body.get("content")),
                asString(body.get("type"))
        );
        send(msg, body);
    }

    /**
     * @param msg  MQ 消息体
     * @param body 原始 Map（降级 Feign 时用）
     */
    private void send(NotificationMessage msg, Map<String, Object> body) {
        if (msg == null || msg.getUserAccount() == null) {
            log.warn("通知消息为空或缺少 userAccount，跳过投递");
            return;
        }
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate 未注入，直接走 Feign 降级: {}", msg.getUserAccount());
            fallbackFeign(body != null ? body : toMap(msg));
            return;
        }
        try {
            rocketMQTemplate.syncSend(TOPIC, MessageBuilder.withPayload(msg).build());
            log.info("通知已投递到 RocketMQ: userAccount={} type={}", msg.getUserAccount(), msg.getType());
        } catch (Exception e) {
            log.warn("RocketMQ 投递失败，降级 Feign 同步调用: {}", msg.getUserAccount(), e);
            fallbackFeign(body != null ? body : toMap(msg));
        }
    }

    /** 降级：通过 Feign 同步调用 notification-service */
    private void fallbackFeign(Map<String, Object> body) {
        if (notificationClient == null) {
            log.error("NotificationClient 未注入，通知丢失: {}", body);
            return;
        }
        try {
            notificationClient.sendNotification(body);
            log.info("降级 Feign 通知发送成功: {}", body.get("userAccount"));
        } catch (Exception ex) {
            log.error("降级 Feign 通知发送也失败: {}", body.get("userAccount"), ex);
        }
    }

    private Map<String, Object> toMap(NotificationMessage msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("userAccount", msg.getUserAccount());
        m.put("title", msg.getTitle());
        m.put("content", msg.getContent());
        m.put("type", msg.getType());
        return m;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
