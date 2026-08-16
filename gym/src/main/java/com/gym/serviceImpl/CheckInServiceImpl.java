package com.gym.serviceImpl;

import com.gym.entity.CheckIn;
import com.gym.entity.ClassOrder;
import com.gym.mapper.CheckInMapper;
import com.gym.mapper.ClassOrderMapper;
import com.gym.service.CheckInService;
import com.gym.service.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CheckInServiceImpl implements CheckInService {

    /** 学员课程签到最晚时间（开课后 4 小时内） */
    private static final int MEMBER_CHECKIN_LIMIT_HOURS = 4;

    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private TimeService timeService;

    @Override
    public List<CheckIn> getAllCheckIns() {
        return checkInMapper.getAllCheckIns();
    }

    @Override
    public List<CheckIn> getCheckInsByMemberAccount(Integer memberAccount) {
        return checkInMapper.getCheckInsByMemberAccount(memberAccount);
    }

    @Override
    public CheckIn getTodayCheckIn(Integer memberAccount) {
        return checkInMapper.getTodayCheckIn(memberAccount);
    }

    @Override
    public CheckIn getClassCheckInByOrder(Integer classOrderId) {
        return checkInMapper.getClassCheckInByOrder(classOrderId);
    }

    /**
     * 签到逻辑：
     * - GYM（自主训练）：每天仅一次，与课程签到状态独立
     * - CLASS（课程签到）：按订单独立，需在开课后 4 小时内，签到后订单转 CHECKED_IN
     */
    @Override
    @Transactional
    public int checkIn(Integer memberAccount, String memberName, Integer classOrderId, String checkInType) {
        if ("CLASS".equals(checkInType)) {
            return classCheckIn(memberAccount, memberName, classOrderId);
        }
        // 默认 GYM 自主训练签到
        CheckIn today = checkInMapper.getTodayCheckIn(memberAccount);
        if (today != null) {
            return -1; // 今日已签到
        }
        CheckIn checkIn = new CheckIn();
        checkIn.setMemberAccount(memberAccount);
        checkIn.setMemberName(memberName);
        checkIn.setCheckInType("GYM");
        checkInMapper.addCheckIn(checkIn);
        return 1;
    }

    private int classCheckIn(Integer memberAccount, String memberName, Integer classOrderId) {
        if (classOrderId == null) {
            return -6; // 缺少 classOrderId
        }
        // 该订单是否已签到
        if (checkInMapper.getClassCheckInByOrder(classOrderId) != null) {
            return -2;
        }
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null || !"BOOKED".equals(order.getStatus())) {
            return -3; // 订单不存在或状态不可签到
        }
        if (order.getClassBegin() == null) {
            return -4;
        }
        LocalDateTime now = timeService.nowDateTime();
        if (now.isBefore(order.getClassBegin())) {
            return -4; // 未到开课时间
        }
        long hoursAfterBegin = ChronoUnit.HOURS.between(order.getClassBegin(), now);
        if (hoursAfterBegin > MEMBER_CHECKIN_LIMIT_HOURS) {
            return -5; // 超过开课 4 小时
        }
        CheckIn checkIn = new CheckIn();
        checkIn.setMemberAccount(memberAccount);
        checkIn.setMemberName(memberName);
        checkIn.setClassOrderId(classOrderId);
        checkIn.setCheckInType("CLASS");
        checkInMapper.addCheckIn(checkIn);
        // 学员签到后订单直接标记为已签到，教练无需操作
        classOrderMapper.updateStatus(classOrderId, "CHECKED_IN");
        return 1;
    }
}
