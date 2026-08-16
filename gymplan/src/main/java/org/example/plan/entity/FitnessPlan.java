package org.example.plan.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 健身计划主表
 */
@Data
public class FitnessPlan {
    private Integer planId;
    private Integer memberAccount;
    private String planName;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 状态：ACTIVE-进行中 / COMPLETED-已完成 / ARCHIVED-已归档 */
    private String status;
    private LocalDateTime createTime;
    /** 训练项明细（非持久化，用于联表查询返回） */
    private List<PlanItem> items;
}
