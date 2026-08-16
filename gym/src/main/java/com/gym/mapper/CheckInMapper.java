package com.gym.mapper;

import com.gym.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CheckInMapper {
    List<CheckIn> getAllCheckIns();
    List<CheckIn> getCheckInsByMemberAccount(Integer memberAccount);
    /** 今日自主训练签到（type=GYM），每天仅一次 */
    CheckIn getTodayCheckIn(Integer memberAccount);
    /** 按预约订单查询课程签到记录（type=CLASS），每订单仅一次 */
    CheckIn getClassCheckInByOrder(Integer classOrderId);
    void addCheckIn(CheckIn checkIn);
}
