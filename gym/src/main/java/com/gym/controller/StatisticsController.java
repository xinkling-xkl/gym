package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.mapper.CheckInMapper;
import com.gym.mapper.ClassOrderMapper;
import com.gym.mapper.EquipmentMapper;
import com.gym.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private CheckInMapper checkInMapper;

    @GetMapping("/overview")
    @SentinelResource(value = "stats-overview", blockHandler = "handleBlock")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalMembers", memberMapper.getAllMembers().size());
        data.put("totalEquipments", equipmentMapper.getAllEquipments().size());
        data.put("totalOrders", classOrderMapper.getAllOrders().size());
        data.put("totalCheckIns", checkInMapper.getAllCheckIns().size());
        return Result.success(data);
    }

    @GetMapping("/today")
    @SentinelResource(value = "stats-today", blockHandler = "handleBlock")
    public Result<Map<String, Object>> today() {
        List<Map<String, Object>> allCheckIns = new ArrayList<>();
        for (var ci : checkInMapper.getAllCheckIns()) {
            Map<String, Object> map = new HashMap<>();
            map.put("checkInTime", ci.getCheckInTime());
            allCheckIns.add(map);
        }

        // 统计今日签到数
        long todayCheckIns = allCheckIns.stream()
                .filter(m -> {
                    Date d = (Date) m.get("checkInTime");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    Calendar today = Calendar.getInstance();
                    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                            && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
                }).count();

        Map<String, Object> data = new HashMap<>();
        data.put("todayCheckIns", todayCheckIns);
        data.put("todayOrders", classOrderMapper.getAllOrders().size()); // 简化：可加日期过滤
        return Result.success(data);
    }

    @GetMapping("/member-growth")
    @SentinelResource(value = "stats-member-growth", blockHandler = "handleBlock")
    public Result<List<Map<String, Object>>> memberGrowth() {
        // 按会员 card_time 字段统计增长（简化版：统计各卡种人数分布代表增长）
        List<Map<String, Object>> result = new ArrayList<>();
        var members = memberMapper.getAllMembers();

        Map<String, Integer> byCardTime = new LinkedHashMap<>();
        for (var m : members) {
            String key = m.getCardTime() != null ? m.getCardTime() : "未知";
            byCardTime.merge(key, 1, Integer::sum);
        }
        for (var entry : byCardTime.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/equipment-status")
    @SentinelResource(value = "stats-equipment", blockHandler = "handleBlock")
    public Result<Map<String, Integer>> equipmentStatus() {
        var list = equipmentMapper.getAllEquipments();
        Map<String, Integer> statusMap = new HashMap<>();
        for (var e : list) {
            String status = e.getEquipmentStatus() != null ? e.getEquipmentStatus() : "未知";
            statusMap.merge(status, 1, Integer::sum);
        }
        return Result.success(statusMap);
    }

    @GetMapping("/course-hot")
    @SentinelResource(value = "stats-course-hot", blockHandler = "handleBlock")
    public Result<List<Map<String, Object>>> courseHot() {
        // 按课程预约数统计热门课程
        var orders = classOrderMapper.getAllOrders();
        Map<Integer, Integer> classCount = new HashMap<>();
        for (var o : orders) {
            classCount.merge(o.getClassId(), 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : classCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("classId", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")));
        return Result.success(result);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
