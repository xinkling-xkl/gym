package com.gym.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassTable {
    private Integer classId;
    private String className;
    private LocalDateTime classBegin;
    private String classTime;
    private String coach;
    private Integer maxCapacity;
    /** 已预约人数（非持久化字段，仅用于前端展示） */
    private Integer bookedCount;
}
