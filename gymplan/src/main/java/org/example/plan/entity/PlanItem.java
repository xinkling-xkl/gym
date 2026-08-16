package org.example.plan.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练项明细表
 */
@Data
public class PlanItem {
    private Integer itemId;
    private Integer planId;
    /** 星期几：1-7 对应 周一到周日 */
    private Integer dayOfWeek;
    /** 关联课程ID（可选，若绑定具体课程） */
    private Integer classId;
    /** 训练项目名称 */
    private String exercise;
    /** 训练时长（分钟） */
    private Integer duration;
    /** 组数 */
    private Integer sets;
    /** 次数 */
    private Integer reps;
    /** 备注 */
    private String notes;
    /** 是否完成：0-未完成 1-已完成 */
    private Integer completed;
    /** 计划执行时间（同步自课程开课时间） */
    private LocalDateTime scheduledTime;
    /** 教练姓名（同步自课程） */
    private String coachName;
}
