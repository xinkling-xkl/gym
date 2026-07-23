package com.gym.service;

import com.gym.entity.ClassOrder;

import java.util.List;

public interface ClassOrderService {
    List<ClassOrder> getAllOrders();
    ClassOrder getOrderById(Integer classOrderId);
    List<ClassOrder> getOrdersByMemberAccount(String memberAccount);
    int addOrder(ClassOrder classOrder);
    void updateOrder(ClassOrder classOrder);
    void deleteOrder(Integer classOrderId);
    boolean cancelOrder(Integer classOrderId);
}
