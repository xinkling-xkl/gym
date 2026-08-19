package org.example.plan.service;

import org.example.plan.dto.SyncResult;
import org.example.plan.entity.FitnessPlan;
import org.example.plan.entity.PlanItem;

import java.util.List;

public interface FitnessPlanService {
    // ===== 计划 =====
    List<FitnessPlan> getPlansByMember(Integer memberAccount);
    FitnessPlan getPlanById(Integer planId);
    boolean addPlan(FitnessPlan plan);
    /**
     * 创建计划及其训练项明细（一次性批量创建，事务内完成）
     * @param plan 计划对象，items 字段需填充训练项列表
     * @return 创建后的计划（planId 已回填）
     */
    FitnessPlan createPlanWithItems(FitnessPlan plan);
    boolean updatePlan(FitnessPlan plan);
    boolean updatePlanStatus(Integer planId, String status);
    boolean deletePlan(Integer planId);

    // ===== 训练项 =====
    boolean addItem(PlanItem item);
    boolean updateItem(PlanItem item);
    boolean deleteItem(Integer itemId);
    boolean toggleItemCompleted(Integer itemId, Integer completed);

    // ===== 同步课程订单 =====
    /**
     * 从 gym 主服务同步会员课程订单到指定计划（差量同步）
     * - 新预约（BOOKED）的课程插入训练项
     * - 已取消/旷课（CANCELLED/NO_SHOW）的课程训练项删除
     * - 已签到/已完成（CHECKED_IN/COMPLETED）的课程训练项保留
     * @param planId 目标计划ID
     * @param memberAccount 会员账号
     * @return 同步结果（新增数、移除数）
     */
    SyncResult syncFromOrders(Integer planId, Integer memberAccount);

    /**
     * 自动同步：将会员课程订单同步到健身计划
     * 若会员没有计划则自动创建一个默认计划（ACTIVE）
     * @param memberAccount 会员账号
     * @return 同步结果（新增数、移除数）
     */
    SyncResult autoSyncOrders(Integer memberAccount);
}
