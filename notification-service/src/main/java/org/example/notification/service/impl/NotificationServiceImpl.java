package org.example.notification.service.impl;

import com.alibaba.fastjson.JSON;
import org.example.notification.entity.Notification;
import org.example.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String NOTIFICATION_KEY_PREFIX = "notification:";
    private static final String USER_NOTIFICATION_KEY_PREFIX = "user:notifications:";

    @Override
    public void sendNotification(Notification notification) {
        // 设置通知ID和创建时间
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID().toString());
        }
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(LocalDateTime.now());
        }
        if (notification.getRead() == null) {
            notification.setRead(false);
        }

        // 保存通知到Redis
        String notificationKey = NOTIFICATION_KEY_PREFIX + notification.getId();
        redisTemplate.opsForValue().set(notificationKey, JSON.toJSONString(notification));

        // 添加到用户通知列表
        String userKey = USER_NOTIFICATION_KEY_PREFIX + notification.getUserAccount();
        redisTemplate.opsForList().leftPush(userKey, notification.getId());

        // 限制每个用户最多保存100条通知
        redisTemplate.opsForList().trim(userKey, 0, 99);

        // 通过WebSocket推送通知
        messagingTemplate.convertAndSend("/topic/notification/" + notification.getUserAccount(), notification);
    }

    @Override
    public List<Notification> getUnreadNotifications(String userAccount) {
        List<Notification> notifications = getAllNotifications(userAccount);
        return notifications.stream()
                .filter(n -> !n.getRead())
                .toList();
    }

    @Override
    public List<Notification> getAllNotifications(String userAccount) {
        String userKey = USER_NOTIFICATION_KEY_PREFIX + userAccount;
        List<Object> notificationIds = redisTemplate.opsForList().range(userKey, 0, -1);
        
        List<Notification> notifications = new ArrayList<>();
        if (notificationIds != null) {
            for (Object idObj : notificationIds) {
                String id = String.valueOf(idObj);
                String notificationKey = NOTIFICATION_KEY_PREFIX + id;
                Object value = redisTemplate.opsForValue().get(notificationKey);
                if (value != null) {
                    Notification notification = JSON.parseObject(String.valueOf(value), Notification.class);
                    notifications.add(notification);
                }
            }
        }
        return notifications;
    }

    @Override
    public void markAsRead(String notificationId) {
        String notificationKey = NOTIFICATION_KEY_PREFIX + notificationId;
        Object value = redisTemplate.opsForValue().get(notificationKey);
        if (value != null) {
            Notification notification = JSON.parseObject(String.valueOf(value), Notification.class);
            notification.setRead(true);
            redisTemplate.opsForValue().set(notificationKey, JSON.toJSONString(notification));
        }
    }

    @Override
    public void markAllAsRead(String userAccount) {
        String userKey = USER_NOTIFICATION_KEY_PREFIX + userAccount;
        List<Object> notificationIds = redisTemplate.opsForList().range(userKey, 0, -1);
        
        if (notificationIds != null) {
            for (Object idObj : notificationIds) {
                String id = String.valueOf(idObj);
                markAsRead(id);
            }
        }
    }

    @Override
    public void deleteNotification(String notificationId) {
        // 删除通知详情
        String notificationKey = NOTIFICATION_KEY_PREFIX + notificationId;
        redisTemplate.delete(notificationKey);
    }
}