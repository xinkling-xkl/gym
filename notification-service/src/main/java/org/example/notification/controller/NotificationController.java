package org.example.notification.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.example.notification.common.Result;
import org.example.notification.entity.Notification;
import org.example.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/unread/{userAccount}")
    @SentinelResource(value = "notification-unread", blockHandler = "handleBlock")
    public Result<List<Notification>> getUnreadNotifications(@PathVariable String userAccount) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userAccount);
        return Result.success(notifications);
    }

    @GetMapping("/all/{userAccount}")
    @SentinelResource(value = "notification-all", blockHandler = "handleBlock")
    public Result<List<Notification>> getAllNotifications(@PathVariable String userAccount) {
        List<Notification> notifications = notificationService.getAllNotifications(userAccount);
        return Result.success(notifications);
    }

    @PostMapping("/read/{notificationId}")
    @SentinelResource(value = "notification-read", blockHandler = "handleBlock")
    public Result<Void> markAsRead(@PathVariable String notificationId) {
        notificationService.markAsRead(notificationId);
        return Result.success("已标记为已读", null);
    }

    @PostMapping("/read-all/{userAccount}")
    @SentinelResource(value = "notification-read-all", blockHandler = "handleBlock")
    public Result<Void> markAllAsRead(@PathVariable String userAccount) {
        notificationService.markAllAsRead(userAccount);
        return Result.success("所有通知已标记为已读", null);
    }

    @PostMapping("/send")
    @SentinelResource(value = "notification-send", blockHandler = "handleBlock")
    public Result<Void> sendNotification(@RequestBody Notification notification) {
        notificationService.sendNotification(notification);
        return Result.success("通知已发送", null);
    }

    @DeleteMapping("/{notificationId}")
    @SentinelResource(value = "notification-delete", blockHandler = "handleBlock")
    public Result<Void> deleteNotification(@PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return Result.success("通知已删除", null);
    }

    public Result<Void> handleBlock(com.alibaba.csp.sentinel.slots.block.BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}