package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server")
public interface ClassOrderClient {

    @GetMapping("/api/order/list")
    Map<String, Object> getAllOrders();

    @GetMapping("/api/order/{id}")
    Map<String, Object> getOrder(@PathVariable("id") Integer id);

    @GetMapping("/api/order/member/{account}")
    Map<String, Object> getOrdersByMemberAccount(@PathVariable("account") String account);

    @PostMapping("/api/order/add")
    Map<String, Object> addOrder(@RequestBody Map<String, Object> order);

    @PutMapping("/api/order/update")
    Map<String, Object> updateOrder(@RequestBody Map<String, Object> order);

    @DeleteMapping("/api/order/delete/{id}")
    Map<String, Object> deleteOrder(@PathVariable("id") Integer id);
}
