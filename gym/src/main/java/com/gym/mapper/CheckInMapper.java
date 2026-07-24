package com.gym.mapper;

import com.gym.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CheckInMapper {
    List<CheckIn> getAllCheckIns();
    List<CheckIn> getCheckInsByMemberAccount(Integer memberAccount);
    CheckIn getTodayCheckIn(Integer memberAccount);
    void addCheckIn(CheckIn checkIn);
}
