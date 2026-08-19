package org.example.notification.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.notification.dto.NotificationMessage;
import org.example.notification.entity.Notification;
import org.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者：消费 gym 通过 RocketMQ 投递的通知，落库 + WebSocket 实时推送。
 * RocketMQ 自带消费重试，broker 端持久化，notification-service 短暂宕机期间消息不丢。
 */
@Component
@RocketMQMessageListener(
        topic = "notify-topic",
        consumerGroup = "notify-consumer-group",
        selectorExpression = "*"
)
public class NotificationConsumer implements RocketMQListener<NotificationMessage> {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @Override
    public void onMessage(NotificationMessage msg) {
        if (msg == null || msg.getUserAccount() == null) {
            log.warn("收到空通知消息或缺少 userAccount，跳过: {}", msg);
            return;
        }
        try {
            Notification notification = Notification.builder()
                    .userAccount(msg.getUserAccount())
                    .title(msg.getTitle())
                    .content(msg.getContent())
                    .type(msg.getType())
                    .read(false)
                    .build();
            notificationService.sendNotification(notification);
            log.debug("通知消费成功 userAccount={} type={}", msg.getUserAccount(), msg.getType());
        } catch (Exception e) {
            log.error("通知消费失败，消息将重试: {}", msg, e);
            // 抛出异常触发 RocketMQ 重试（默认 16 次）
            throw new RuntimeException(e);
        }
    }
}
