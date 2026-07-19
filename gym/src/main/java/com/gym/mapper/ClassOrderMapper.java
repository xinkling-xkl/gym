package com.gym.mapper;

import com.gym.entity.ClassOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClassOrderMapper {
    List<ClassOrder> getAllOrders();
    ClassOrder getOrderById(Integer classOrderId);
    List<ClassOrder> getOrdersByMemberAccount(String memberAccount);
    void addOrder(ClassOrder classOrder);
    void updateOrder(ClassOrder classOrder);
    void deleteOrder(Integer classOrderId);
}
