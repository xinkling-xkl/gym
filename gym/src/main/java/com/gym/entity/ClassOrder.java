package com.gym.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassOrder {
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
