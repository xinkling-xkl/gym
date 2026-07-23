package com.gym.serviceImpl;

import com.gym.entity.ClassOrder;
import com.gym.entity.Member;
import com.gym.mapper.ClassOrderMapper;
import com.gym.mapper.MemberMapper;
import com.gym.service.ClassOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassOrderServiceImpl implements ClassOrderService {

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private MemberMapper memberMapper;

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
    @Transactional
    public int addOrder(ClassOrder classOrder) {
        // 1. 查询会员剩余课时
        Integer account = Integer.parseInt(classOrder.getMemberAccount());
        Member member = memberMapper.getMemberByAccount(account);
        if (member == null) {
            return -2; // 会员不存在
        }
        if (member.getCardNextClass() == null || member.getCardNextClass() <= 0) {
            return -1; // 剩余课时不足
        }
        // 2. 扣减课时
        int deducted = memberMapper.deductClass(account);
        if (deducted <= 0) {
            return -1; // 扣减失败（可能并发导致）
        }
        // 3. 创建订单
        classOrderMapper.addOrder(classOrder);
        return 1; // 成功
    }

    @Override
    public void updateOrder(ClassOrder classOrder) {
        classOrderMapper.updateOrder(classOrder);
    }

    @Override
    public void deleteOrder(Integer classOrderId) {
        classOrderMapper.deleteOrder(classOrderId);
    }

    @Override
    @Transactional
    public boolean cancelOrder(Integer classOrderId) {
        // 1. 查询订单
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null) {
            return false;
        }
        // 2. 退回课时
        Integer account = Integer.parseInt(order.getMemberAccount());
        memberMapper.refundClass(account);
        // 3. 删除订单
        classOrderMapper.deleteOrder(classOrderId);
        return true;
    }
}
