package org.example.notification.service;

import org.example.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    /**
     * 发送通知
     */
    void sendNotification(Notification notification);

    /**
     * 获取用户未读通知
     */
    List<Notification> getUnreadNotifications(String userAccount);

    /**
     * 获取用户所有通知
     */
    List<Notification> getAllNotifications(String userAccount);

    /**
     * 标记通知为已读
     */
    void markAsRead(String notificationId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(String userAccount);

    /**
     * 删除通知
     */
    void deleteNotification(String notificationId);
}