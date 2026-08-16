package org.example.plan.client;

import org.example.plan.common.Result;
import org.example.plan.dto.ClassOrderDTO;
import org.example.plan.dto.ClassTableDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign 客户端：调用 gym 主服务（注册名 main-server）获取会员课程订单
 */
@FeignClient(name = "main-server", contextId = "gymClient")
public interface GymClient {

    /** 查询会员的所有订单 */
    @GetMapping("/api/order/member/{account}")
    Result<List<ClassOrderDTO>> getOrdersByMember(@PathVariable("account") String account);

    /** 按状态查询会员订单（status: BOOKED / COMPLETED / NO_SHOW） */
    @GetMapping("/api/order/member/{account}/{status}")
    Result<List<ClassOrderDTO>> getOrdersByMemberAndStatus(@PathVariable("account") String account,
                                                           @PathVariable("status") String status);

    /** 按课程ID查询课程详情（获取课程时长等） */
    @GetMapping("/api/class/{id}")
    Result<ClassTableDTO> getClassById(@PathVariable("id") Integer id);
}
