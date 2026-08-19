package org.example.plan.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.example.plan.common.Result;
import org.example.plan.dto.SyncResult;
import org.example.plan.entity.FitnessPlan;
import org.example.plan.entity.PlanItem;
import org.example.plan.service.FitnessPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员健身计划管理接口
 * 限流异常由 GlobalExceptionHandler 统一处理，无需 blockHandler
 */
@RestController
@RequestMapping("/api/plan")
@CrossOrigin(origins = "*")
public class FitnessPlanController {

    @Autowired
    private FitnessPlanService fitnessPlanService;

    // ==================== 计划 ====================

    /** 查询会员的所有计划（含训练项明细） */
    @GetMapping("/member/{account}")
    @SentinelResource(value = "plan-list")
    public Result<List<FitnessPlan>> getPlansByMember(@PathVariable Integer account) {
        List<FitnessPlan> plans = fitnessPlanService.getPlansByMember(account);
        return Result.success(plans);
    }

    /** 查询单个计划详情 */
    @GetMapping("/{planId}")
    @SentinelResource(value = "plan-get")
    public Result<FitnessPlan> getPlanById(@PathVariable Integer planId) {
        FitnessPlan plan = fitnessPlanService.getPlanById(planId);
        if (plan != null) {
            return Result.success(plan);
        }
        return Result.error(404, "计划不存在");
    }

    /** 新增计划 */
    @PostMapping("/add")
    @SentinelResource(value = "plan-add")
    public Result<Void> addPlan(@RequestBody FitnessPlan plan) {
        boolean ok = fitnessPlanService.addPlan(plan);
        return ok ? Result.success("创建计划成功", null)
                  : Result.error(500, "创建计划失败");
    }

    /** 创建计划及其训练项明细（一次性批量创建，供 AI 智能生成计划使用） */
    @PostMapping("/createWithItems")
    @SentinelResource(value = "plan-create-with-items")
    public Result<FitnessPlan> createPlanWithItems(@RequestBody FitnessPlan plan) {
        try {
            FitnessPlan created = fitnessPlanService.createPlanWithItems(plan);
            return Result.success("创建成功", created);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    /** 更新计划基础信息 */
    @PutMapping("/update")
    @SentinelResource(value = "plan-update")
    public Result<Void> updatePlan(@RequestBody FitnessPlan plan) {
        boolean ok = fitnessPlanService.updatePlan(plan);
        return ok ? Result.success("更新成功", null)
                  : Result.error(500, "更新失败");
    }

    /** 更新计划状态（ACTIVE / COMPLETED / ARCHIVED） */
    @PutMapping("/status/{planId}")
    @SentinelResource(value = "plan-status")
    public Result<Void> updateStatus(@PathVariable Integer planId, @RequestParam String status) {
        boolean ok = fitnessPlanService.updatePlanStatus(planId, status);
        return ok ? Result.success("状态更新成功", null)
                  : Result.error(500, "状态更新失败");
    }

    /** 删除计划（级联删除训练项） */
    @DeleteMapping("/delete/{planId}")
    @SentinelResource(value = "plan-delete")
    public Result<Void> deletePlan(@PathVariable Integer planId) {
        boolean ok = fitnessPlanService.deletePlan(planId);
        return ok ? Result.success("删除成功", null)
                  : Result.error(404, "计划不存在");
    }

    // ==================== 训练项 ====================

    /** 新增训练项 */
    @PostMapping("/item/add")
    @SentinelResource(value = "plan-item-add")
    public Result<Void> addItem(@RequestBody PlanItem item) {
        boolean ok = fitnessPlanService.addItem(item);
        return ok ? Result.success("添加训练项成功", null)
                  : Result.error(500, "添加失败");
    }

    /** 更新训练项 */
    @PutMapping("/item/update")
    @SentinelResource(value = "plan-item-update")
    public Result<Void> updateItem(@RequestBody PlanItem item) {
        boolean ok = fitnessPlanService.updateItem(item);
        return ok ? Result.success("更新成功", null)
                  : Result.error(500, "更新失败");
    }

    /** 删除训练项 */
    @DeleteMapping("/item/delete/{itemId}")
    @SentinelResource(value = "plan-item-delete")
    public Result<Void> deleteItem(@PathVariable Integer itemId) {
        boolean ok = fitnessPlanService.deleteItem(itemId);
        return ok ? Result.success("删除成功", null)
                  : Result.error(404, "训练项不存在");
    }

    /** 切换训练项完成状态：completed=1 标记完成，0 标记未完成 */
    @PutMapping("/item/toggle/{itemId}")
    @SentinelResource(value = "plan-item-toggle")
    public Result<Void> toggleCompleted(@PathVariable Integer itemId, @RequestParam Integer completed) {
        boolean ok = fitnessPlanService.toggleItemCompleted(itemId, completed);
        return ok ? Result.success("状态切换成功", null)
                  : Result.error(404, "训练项不存在");
    }

    // ==================== 同步课程订单 ====================

    /**
     * 一键同步会员课程订单到指定计划（差量同步）
     * 新预约的课程插入训练项，已取消/旷课的课程训练项自动移除
     */
    @PostMapping("/sync/{planId}/{account}")
    @SentinelResource(value = "plan-sync")
    public Result<SyncResult> syncFromOrders(@PathVariable Integer planId, @PathVariable Integer account) {
        try {
            SyncResult result = fitnessPlanService.syncFromOrders(planId, account);
            StringBuilder msg = new StringBuilder();
            if (result.getInserted() > 0) {
                msg.append("新增 ").append(result.getInserted()).append(" 项训练项");
            }
            if (result.getRemoved() > 0) {
                if (msg.length() > 0) msg.append("，");
                msg.append("移除 ").append(result.getRemoved()).append(" 项已取消课程的训练项");
            }
            if (msg.length() == 0) {
                msg.append("暂无可同步的变更（已预约课程均已同步）");
            }
            return Result.success(msg.toString(), result);
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 自动同步课程订单到健身计划（无计划则自动创建）
     * 供 AI 助手预约课程成功后调用
     */
    @PostMapping("/autoSync/{account}")
    @SentinelResource(value = "plan-auto-sync")
    public Result<SyncResult> autoSync(@PathVariable Integer account) {
        try {
            SyncResult result = fitnessPlanService.autoSyncOrders(account);
            String msg = result.getInserted() > 0
                    ? "自动同步成功，新增 " + result.getInserted() + " 项训练项"
                    : "暂无可同步的新课程";
            return Result.success(msg, result);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }
    }
}
