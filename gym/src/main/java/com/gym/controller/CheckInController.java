package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.CheckIn;
import com.gym.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = "*")
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    @GetMapping("/list")
    @SentinelResource(value = "checkin-list", blockHandler = "handleBlock")
    public Result<List<CheckIn>> getAllCheckIns() {
        return Result.success(checkInService.getAllCheckIns());
    }

    @GetMapping("/member/{account}")
    @SentinelResource(value = "checkin-member", blockHandler = "handleBlock")
    public Result<List<CheckIn>> getByMember(@PathVariable Integer account) {
        return Result.success(checkInService.getCheckInsByMemberAccount(account));
    }

    @GetMapping("/today/{account}")
    @SentinelResource(value = "checkin-today", blockHandler = "handleBlock")
    public Result<CheckIn> getTodayCheckIn(@PathVariable Integer account) {
        CheckIn checkIn = checkInService.getTodayCheckIn(account);
        return Result.success(checkIn);
    }

    @GetMapping("/class/{orderId}")
    @SentinelResource(value = "checkin-class", blockHandler = "handleBlock")
    public Result<CheckIn> getClassCheckIn(@PathVariable Integer orderId) {
        return Result.success(checkInService.getClassCheckInByOrder(orderId));
    }

    @PostMapping
    @SentinelResource(value = "checkin-add", blockHandler = "handleBlock")
    public Result<String> checkIn(@RequestBody Map<String, Object> body) {
        Integer memberAccount = Integer.parseInt(body.get("memberAccount").toString());
        String memberName = (String) body.get("memberName");
        Object classOrderIdObj = body.get("classOrderId");
        Integer classOrderId = classOrderIdObj != null ? Integer.parseInt(classOrderIdObj.toString()) : null;
        String checkInType = (String) body.getOrDefault("checkInType", "GYM");

        int result = checkInService.checkIn(memberAccount, memberName, classOrderId, checkInType);
        switch (result) {
            case 1: return Result.success("签到成功", null);
            case -1: return Result.error(400, "今日已自主训练签到，无需重复签到");
            case -2: return Result.error(400, "该课程已签到，无需重复签到");
            case -3: return Result.error(400, "订单不存在或状态不可签到");
            case -4: return Result.error(400, "未到开课时间，无法签到");
            case -5: return Result.error(400, "已超过开课4小时，无法签到");
            case -6: return Result.error(400, "课程签到缺少订单ID");
            default: return Result.error(400, "签到失败");
        }
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
