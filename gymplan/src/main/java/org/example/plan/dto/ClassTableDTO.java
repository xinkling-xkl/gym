package org.example.plan.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接收 gym 主服务返回的课程数据
 * 字段与 com.gym.entity.ClassTable 保持一致
 */
@Data
public class ClassTableDTO {
    private Integer classId;
    private String className;
    private LocalDateTime classBegin;
    /** 课程时长（字符串，如"60分钟"、"90分钟"） */
    private String classTime;
    private String coach;
    private Integer maxCapacity;
}
