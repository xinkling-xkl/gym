package com.gym.serviceImpl;

import com.gym.entity.CheckIn;
import com.gym.mapper.CheckInMapper;
import com.gym.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CheckInServiceImpl implements CheckInService {

    @Autowired
    private CheckInMapper checkInMapper;

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
    public boolean checkIn(Integer memberAccount, String memberName, Integer classOrderId, String checkInType) {
        CheckIn today = checkInMapper.getTodayCheckIn(memberAccount);
        if (today != null) {
            return false; // 今日已签到
        }
        CheckIn checkIn = new CheckIn();
        checkIn.setMemberAccount(memberAccount);
        checkIn.setMemberName(memberName);
        checkIn.setClassOrderId(classOrderId);
        checkIn.setCheckInType(checkInType);
        checkInMapper.addCheckIn(checkIn);
        return true;
    }
}
