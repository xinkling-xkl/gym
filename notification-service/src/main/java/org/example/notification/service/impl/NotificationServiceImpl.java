package org.example.notification.service.impl;

import org.example.notification.entity.Notification;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.service.NotificationService;
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

    @Override
    public void sendNotification(Notification notification) {
        if (notification.getRead() == null) {
            notification.setRead(false);
        }
        // 存入 MySQL，create_time 由数据库自动填充，id 自动回填
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
