package org.example.plan.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接收 gym 主服务返回的课程订单数据
 * 字段与 com.gym.entity.ClassOrder 保持一致
 */
@Data
public class ClassOrderDTO {
    private Integer classOrderId;
    private Integer classId;
    private String className;
    private String coach;
    private String memberName;
    private String memberAccount;
    private LocalDateTime classBegin;
    private String status;
    private LocalDateTime createTime;
}
