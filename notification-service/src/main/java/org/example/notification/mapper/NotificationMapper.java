package org.example.notification.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.notification.entity.Notification;

import java.util.List;

public interface NotificationMapper {

    /**
     * 插入通知（create_time 由数据库自动填充）
     */
    int insertNotification(Notification notification);

    /**
     * 查询用户所有通知（按创建时间倒序）
     */
    List<Notification> selectByUserAccount(@Param("userAccount") String userAccount);

    /**
     * 查询用户未读通知
     */
    List<Notification> selectUnreadByUserAccount(@Param("userAccount") String userAccount);

    /**
     * 标记单条通知为已读
     */
    int markAsRead(@Param("id") Long id);

    /**
     * 标记用户所有通知为已读
     */
    int markAllAsRead(@Param("userAccount") String userAccount);

    /**
     * 删除单条通知
     */
    int deleteById(@Param("id") Long id);
}
