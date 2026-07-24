package com.gym.service;

import com.gym.entity.CheckIn;

import java.util.List;

public interface CheckInService {
    List<CheckIn> getAllCheckIns();
    List<CheckIn> getCheckInsByMemberAccount(Integer memberAccount);
    CheckIn getTodayCheckIn(Integer memberAccount);
    boolean checkIn(Integer memberAccount, String memberName, Integer classOrderId, String checkInType);
}
