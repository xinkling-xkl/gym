package org.example.notification.service.impl;

import org.example.notification.entity.Notification;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.service.NotificationService;
import org.example.notification.service.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TimeService timeService;

    @Override
    public void sendNotification(Notification notification) {
        if (notification.getRead() == null) {
            notification.setRead(false);
        }
        // create_time 由 Java 代码显式设置，避免 MySQL 时区不一致导致差 8 小时
        notification.setCreateTime(timeService.nowDateTime());
        notificationMapper.insertNotification(notification);

        // 通过 WebSocket 实时推送
        messagingTemplate.convertAndSend(
                "/topic/notification/" + notification.getUserAccount(), notification);
    }

    @Override
    public List<Notification> getUnreadNotifications(String userAccount) {
        return notificationMapper.selectUnreadByUserAccount(userAccount);
    }

    @Override
    public List<Notification> getAllNotifications(String userAccount) {
        return notificationMapper.selectByUserAccount(userAccount);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(String userAccount) {
        notificationMapper.markAllAsRead(userAccount);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationMapper.deleteById(notificationId);
    }
}
