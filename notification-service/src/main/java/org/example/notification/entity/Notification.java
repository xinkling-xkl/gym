package org.example.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * 通知ID
     */
    private String id;

    /**
     * 接收用户ID
     */
    private Integer userId;

    /**
     * 接收用户账号
     */
    private String userAccount;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型: SYSTEM, ORDER, CHECKIN, PROMOTION
     */
    private String type;

    /**
     * 是否已读
     */
    private Boolean read;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}