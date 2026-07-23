package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.ClassOrder;
import com.gym.service.ClassOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class ClassOrderController {

    @Autowired
    private ClassOrderService classOrderService;

    @GetMapping("/list")
    @SentinelResource(value = "order-list", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getAllOrders() {
        List<ClassOrder> orders = classOrderService.getAllOrders();
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "order-get", blockHandler = "handleBlock")
    public Result<ClassOrder> getOrderById(@PathVariable Integer id) {
        ClassOrder order = classOrderService.getOrderById(id);
        if (order != null) {
            return Result.success(order);
        }
        return Result.error(404, "订单不存在");
    }

    @GetMapping("/member/{account}")
    @SentinelResource(value = "order-member-list", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getOrdersByMemberAccount(@PathVariable String account) {
        List<ClassOrder> orders = classOrderService.getOrdersByMemberAccount(account);
        return Result.success(orders);
    }

    @PostMapping("/add")
    @SentinelResource(value = "order-add", blockHandler = "handleBlock")
    public Result<Void> addOrder(@RequestBody ClassOrder classOrder) {
        int result = classOrderService.addOrder(classOrder);
        if (result == 1) {
            return Result.success("预约成功", null);
        } else if (result == -1) {
            return Result.error(400, "剩余课时不足，无法预约");
        } else {
            return Result.error(404, "会员不存在");
        }
    }

    @PutMapping("/update")
    @SentinelResource(value = "order-update", blockHandler = "handleBlock")
    public Result<Void> updateOrder(@RequestBody ClassOrder classOrder) {
        classOrderService.updateOrder(classOrder);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    @SentinelResource(value = "order-delete", blockHandler = "handleBlock")
    public Result<Void> deleteOrder(@PathVariable Integer id) {
        classOrderService.deleteOrder(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/cancel/{id}")
    @SentinelResource(value = "order-cancel", blockHandler = "handleBlock")
    public Result<Void> cancelOrder(@PathVariable Integer id) {
        boolean success = classOrderService.cancelOrder(id);
        if (success) {
            return Result.success("取消预约成功，课时已退回", null);
        }
        return Result.error(404, "订单不存在");
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
