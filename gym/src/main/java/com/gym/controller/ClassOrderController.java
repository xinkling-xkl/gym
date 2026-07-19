package com.gym.controller;

import com.gym.entity.ClassOrder;
import com.gym.service.ClassOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class ClassOrderController {

    @Autowired
    private ClassOrderService classOrderService;

    @GetMapping("/list")
    public Map<String, Object> getAllOrders() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ClassOrder> orders = classOrderService.getAllOrders();
            result.put("code", 200);
            result.put("data", orders);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrderById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ClassOrder order = classOrderService.getOrderById(id);
            if (order != null) {
                result.put("code", 200);
                result.put("data", order);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "订单不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/member/{account}")
    public Map<String, Object> getOrdersByMemberAccount(@PathVariable String account) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ClassOrder> orders = classOrderService.getOrdersByMemberAccount(account);
            result.put("code", 200);
            result.put("data", orders);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addOrder(@RequestBody ClassOrder classOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            classOrderService.addOrder(classOrder);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateOrder(@RequestBody ClassOrder classOrder) {
        Map<String, Object> result = new HashMap<>();
        try {
            classOrderService.updateOrder(classOrder);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteOrder(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            classOrderService.deleteOrder(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
