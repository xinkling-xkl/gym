package com.gym.serviceImpl;

import com.gym.entity.ClassOrder;
import com.gym.mapper.ClassOrderMapper;
import com.gym.service.ClassOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassOrderServiceImpl implements ClassOrderService {

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Override
    public List<ClassOrder> getAllOrders() {
        return classOrderMapper.getAllOrders();
    }

    @Override
    public ClassOrder getOrderById(Integer classOrderId) {
        return classOrderMapper.getOrderById(classOrderId);
    }

    @Override
    public List<ClassOrder> getOrdersByMemberAccount(String memberAccount) {
        return classOrderMapper.getOrdersByMemberAccount(memberAccount);
    }

    @Override
    public void addOrder(ClassOrder classOrder) {
        classOrderMapper.addOrder(classOrder);
    }

    @Override
    public void updateOrder(ClassOrder classOrder) {
        classOrderMapper.updateOrder(classOrder);
    }

    @Override
    public void deleteOrder(Integer classOrderId) {
        classOrderMapper.deleteOrder(classOrderId);
    }
}
