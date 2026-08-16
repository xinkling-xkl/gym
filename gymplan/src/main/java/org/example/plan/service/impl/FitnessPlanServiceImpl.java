package org.example.plan.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.plan.client.GymClient;
import org.example.plan.common.Result;
import org.example.plan.common.UserContext;
import org.example.plan.dto.ClassOrderDTO;
import org.example.plan.dto.ClassTableDTO;
import org.example.plan.entity.FitnessPlan;
import org.example.plan.entity.PlanItem;
import org.example.plan.mapper.FitnessPlanMapper;
import org.example.plan.mapper.PlanItemMapper;
import org.example.plan.service.FitnessPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FitnessPlanServiceImpl implements FitnessPlanService {

    @Autowired
    private FitnessPlanMapper fitnessPlanMapper;

    @Autowired
    private PlanItemMapper planItemMapper;

    @Autowired
    private GymClient gymClient;

    // ==================== 越权校验 ====================

    /**
     * 校验当前登录用户是否有权操作指定计划
     * @throws SecurityException 无权操作时抛出
     */
    private void checkPlanOwnership(Integer planId) {
        String currentUser = UserContext.getCurrentAccount();
        if (currentUser == null) {
            throw new SecurityException("未登录");
        }
        FitnessPlan plan = fitnessPlanMapper.getPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("计划不存在: " + planId);
        }
        if (!String.valueOf(plan.getMemberAccount()).equals(currentUser)) {
            throw new SecurityException("无权操作他人的计划");
        }
    }

    /**
     * 通过训练项ID校验所有权：先查 itemId → planId，再校验 plan 归属
     */
    private void checkItemOwnership(Integer itemId) {
        Integer planId = planItemMapper.getPlanIdByItemId(itemId);
        if (planId == null) {
            throw new IllegalArgumentException("训练项不存在: " + itemId);
        }
        checkPlanOwnership(planId);
    }

    // ==================== 计划 ====================

    @Override
    public List<FitnessPlan> getPlansByMember(Integer memberAccount) {
        // 只允许查询自己的计划
        String currentUser = UserContext.getCurrentAccount();
        if (currentUser != null && !String.valueOf(memberAccount).equals(currentUser)) {
            throw new SecurityException("无权查看他人的计划");
        }
        return fitnessPlanMapper.getPlansByMember(memberAccount);
    }

    @Override
    public FitnessPlan getPlanById(Integer planId) {
        checkPlanOwnership(planId);
        return fitnessPlanMapper.getPlanById(planId);
    }

    @Override
    @Transactional
    public boolean addPlan(FitnessPlan plan) {
        // 新建计划时，memberAccount 必须使用当前登录用户
        String currentUser = UserContext.getCurrentAccount();
        if (currentUser == null) {
            throw new SecurityException("未登录");
        }
        plan.setMemberAccount(Integer.parseInt(currentUser));
        return fitnessPlanMapper.addPlan(plan) > 0;
    }

    @Override
    @Transactional
    public boolean updatePlan(FitnessPlan plan) {
        checkPlanOwnership(plan.getPlanId());
        return fitnessPlanMapper.updatePlan(plan) > 0;
    }

    @Override
    @Transactional
    public boolean updatePlanStatus(Integer planId, String status) {
        checkPlanOwnership(planId);
        return fitnessPlanMapper.updateStatus(planId, status) > 0;
    }

    @Override
    @Transactional
    public boolean deletePlan(Integer planId) {
        checkPlanOwnership(planId);
        planItemMapper.deleteByPlanId(planId);
        return fitnessPlanMapper.deletePlan(planId) > 0;
    }

    // ==================== 训练项 ====================

    @Override
    @Transactional
    public boolean addItem(PlanItem item) {
        checkPlanOwnership(item.getPlanId());
        return planItemMapper.addItem(item) > 0;
    }

    @Override
    @Transactional
    public boolean updateItem(PlanItem item) {
        checkItemOwnership(item.getItemId());
        return planItemMapper.updateItem(item) > 0;
    }

    @Override
    @Transactional
    public boolean deleteItem(Integer itemId) {
        checkItemOwnership(itemId);
        return planItemMapper.deleteItem(itemId) > 0;
    }

    @Override
    @Transactional
    public boolean toggleItemCompleted(Integer itemId, Integer completed) {
        checkItemOwnership(itemId);
        return planItemMapper.toggleCompleted(itemId, completed) > 0;
    }

    // ==================== 同步课程订单 ====================

    @Override
    @Transactional
    public int syncFromOrders(Integer planId, Integer memberAccount) {
        // 校验：计划存在且属于当前登录用户
        checkPlanOwnership(planId);

        // Feign 调用 gym 服务拉取会员的 BOOKED 订单
        Result<List<ClassOrderDTO>> resp;
        try {
            resp = gymClient.getOrdersByMemberAndStatus(String.valueOf(memberAccount), "BOOKED");
        } catch (Exception e) {
            log.error("调用 gym 服务获取订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法连接课程服务，请稍后重试");
        }

        if (resp == null || resp.getCode() != 200 || resp.getData() == null) {
            log.warn("gym 服务返回异常: {}", resp);
            throw new RuntimeException("获取课程订单失败: " + (resp != null ? resp.getMessage() : "服务不可用"));
        }

        List<ClassOrderDTO> orders = resp.getData();
        if (orders.isEmpty()) {
            return 0;
        }

        // 遍历订单，去重后插入
        int inserted = 0;
        for (ClassOrderDTO order : orders) {
            if (order.getClassId() == null) {
                continue;
            }
            // 去重：同一计划下相同 classId 的训练项已存在则跳过
            Integer exists = planItemMapper.countByPlanIdAndClassId(planId, order.getClassId());
            if (exists != null && exists > 0) {
                continue;
            }

            // 查课程详情获取时长
            Integer duration = null;
            try {
                Result<ClassTableDTO> classResp = gymClient.getClassById(order.getClassId());
                if (classResp != null && classResp.getCode() == 200 && classResp.getData() != null) {
                    duration = parseDuration(classResp.getData().getClassTime());
                }
            } catch (Exception e) {
                log.warn("查询课程 {} 时长失败，跳过时长同步: {}", order.getClassId(), e.getMessage());
            }

            PlanItem item = new PlanItem();
            item.setPlanId(planId);
            item.setClassId(order.getClassId());
            item.setExercise(order.getClassName());
            item.setCoachName(order.getCoach());
            item.setScheduledTime(order.getClassBegin());
            item.setDuration(duration);
            item.setCompleted(0);
            LocalDateTime begin = order.getClassBegin();
            if (begin != null) {
                DayOfWeek dow = begin.getDayOfWeek();
                item.setDayOfWeek(dow.getValue());
            }
            item.setNotes("同步自课程预约");

            planItemMapper.addItem(item);
            inserted++;
        }

        log.info("会员 {} 计划 {} 同步课程，新增 {} 项训练项", memberAccount, planId, inserted);
        return inserted;
    }

    /**
     * 从课程时长字符串中解析分钟数
     * 支持格式："60分钟"、"90分钟"、"1小时"、"1.5小时"、"1小时30分钟"
     */
    private Integer parseDuration(String classTime) {
        if (classTime == null || classTime.trim().isEmpty()) {
            return null;
        }
        String s = classTime.trim();

        Pattern minPattern = Pattern.compile("(\\d+)\\s*分钟");
        Matcher minMatcher = minPattern.matcher(s);
        if (minMatcher.find()) {
            return Integer.parseInt(minMatcher.group(1));
        }

        Pattern hourPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*小时");
        Matcher hourMatcher = hourPattern.matcher(s);
        if (hourMatcher.find()) {
            double hours = Double.parseDouble(hourMatcher.group(1));
            return (int) (hours * 60);
        }

        Pattern comboPattern = Pattern.compile("(\\d+)\\s*小时(\\d+)\\s*分钟");
        Matcher comboMatcher = comboPattern.matcher(s);
        if (comboMatcher.find()) {
            int hours = Integer.parseInt(comboMatcher.group(1));
            int mins = Integer.parseInt(comboMatcher.group(2));
            return hours * 60 + mins;
        }

        if (s.matches("\\d+")) {
            return Integer.parseInt(s);
        }

        log.warn("无法解析课程时长: {}", classTime);
        return null;
    }
}
