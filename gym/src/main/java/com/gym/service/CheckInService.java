package com.gym.service;

import com.gym.entity.CheckIn;

import java.util.List;

public interface CheckInService {
    List<CheckIn> getAllCheckIns();
    List<CheckIn> getCheckInsByMemberAccount(Integer memberAccount);
    CheckIn getTodayCheckIn(Integer memberAccount);
    CheckIn getClassCheckInByOrder(Integer classOrderId);
    /**
     * 签到
     * @return 1=成功, -1=GYM今日已签到, -2=该课程已签到, -3=订单不存在/状态不可签到,
     *         -4=未到开课时间, -5=超过签到时限(开课4小时), -6=缺少classOrderId
     */
    int checkIn(Integer memberAccount, String memberName, Integer classOrderId, String checkInType);
}
