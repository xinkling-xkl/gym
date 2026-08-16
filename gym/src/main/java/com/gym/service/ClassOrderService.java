package com.gym.service;

import com.gym.entity.ClassOrder;

import java.util.List;

public interface ClassOrderService {
    List<ClassOrder> getAllOrders();
    ClassOrder getOrderById(Integer classOrderId);
    List<ClassOrder> getOrdersByMemberAccount(String memberAccount);
    List<ClassOrder> getOrdersByMemberAccountAndStatus(String memberAccount, String status);
    List<ClassOrder> getOrdersByCoach(String coach);
    int addOrder(ClassOrder classOrder);
    void updateOrder(ClassOrder classOrder);
    void deleteOrder(Integer classOrderId);
    /** 取消预约：1=成功, -1=订单不存在, -2=状态不可取消, -3=开课前2小时内不可取消 */
    int cancelOrder(Integer classOrderId);
    boolean completeOrder(Integer classOrderId);
    boolean markNoShow(Integer classOrderId);
    int cancelByClassId(Integer classId);
    List<ClassOrder> getBookedOrdersByClassId(Integer classId);
    /** 管理员强制取消订单（不限状态和时间） */
    boolean adminCancelOrder(Integer classOrderId);
}
