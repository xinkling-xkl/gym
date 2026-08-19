package org.example.plan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.plan.entity.PlanItem;

import java.util.List;

@Mapper
public interface PlanItemMapper {
    /** 查询某计划下所有训练项 */
    List<PlanItem> getItemsByPlanId(Integer planId);

    /** 新增训练项 */
    int addItem(PlanItem item);

    /** 更新训练项 */
    int updateItem(PlanItem item);

    /** 删除训练项 */
    int deleteItem(Integer itemId);

    /** 删除某计划下所有训练项（级联删除用） */
    int deleteByPlanId(Integer planId);

    /** 切换训练项完成状态 */
    int toggleCompleted(Integer itemId, Integer completed);

    /** 查询某计划下是否已存在指定课程的训练项（用于同步去重） */
    Integer countByPlanIdAndClassId(Integer planId, Integer classId);

    /** 通过训练项ID查询所属计划ID（用于越权校验） */
    Integer getPlanIdByItemId(Integer itemId);

    /** 删除某计划下所有同步自课程的训练项（class_id 不为空），用于无当前订单时清空 */
    int deleteSyncedItemsByPlanId(Integer planId);

    /** 删除某计划下 class_id 不在指定集合中的同步训练项，用于清理已取消预约的课程 */
    int deleteSyncedItemsNotInClassIds(@Param("planId") Integer planId, @Param("classIds") List<Integer> classIds);
}
