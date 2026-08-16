package org.example.plan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.plan.entity.FitnessPlan;

import java.util.List;

@Mapper
public interface FitnessPlanMapper {
    /** 查询会员的所有计划（不含明细） */
    List<FitnessPlan> getPlansByMember(Integer memberAccount);

    /** 查询单个计划（含明细 items） */
    FitnessPlan getPlanById(Integer planId);

    /** 新增计划 */
    int addPlan(FitnessPlan plan);

    /** 更新计划基础信息 */
    int updatePlan(FitnessPlan plan);

    /** 更新计划状态 */
    int updateStatus(Integer planId, String status);

    /** 删除计划（同时级联删除明细） */
    int deletePlan(Integer planId);
}
