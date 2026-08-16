package org.example.plan.service;

import org.example.plan.entity.FitnessPlan;
import org.example.plan.entity.PlanItem;

import java.util.List;

public interface FitnessPlanService {
    // ===== 计划 =====
    List<FitnessPlan> getPlansByMember(Integer memberAccount);
    FitnessPlan getPlanById(Integer planId);
    boolean addPlan(FitnessPlan plan);
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
     * 从 gym 主服务同步会员已预约课程到指定计划
     * @param planId 目标计划ID
     * @param memberAccount 会员账号
     * @return 新增的训练项数量
     */
    int syncFromOrders(Integer planId, Integer memberAccount);
}
