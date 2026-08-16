package com.gym.mapper;

import com.gym.entity.ClassOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassOrderMapper {
    List<ClassOrder> getAllOrders();
    ClassOrder getOrderById(Integer classOrderId);
    List<ClassOrder> getOrdersByMemberAccount(String memberAccount);
    List<ClassOrder> getOrdersByMemberAccountAndStatus(@Param("memberAccount") String memberAccount,
                                                       @Param("status") String status);
    List<ClassOrder> getOrdersByCoach(String coach);
    int countBookedByClassId(Integer classId);
    /** 该课程下 COMPLETED/NO_SHOW 订单数（用于判断课程是否已被教练处理结束） */
    int countProcessedByClassId(Integer classId);
    List<ClassOrder> getBookedOrdersByClassId(Integer classId);
    void addOrder(ClassOrder classOrder);
    void updateOrder(ClassOrder classOrder);
    void updateStatus(@Param("classOrderId") Integer classOrderId, @Param("status") String status);
    int batchCancelByClassId(Integer classId);
    void deleteOrder(Integer classOrderId);

    /**
     * 课程信息变更时，同步更新该课程下所有 BOOKED 状态订单的快照字段
     * （class_name / class_begin / coach），仅影响未开课的预约，历史订单保持不变。
     */
    int syncBookedByClassId(@Param("classId") Integer classId,
                            @Param("className") String className,
                            @Param("classBegin") java.time.LocalDateTime classBegin,
                            @Param("coach") String coach);
}
