package com.gym.entity;

import lombok.Data;

import java.util.Date;

@Data
public class CheckIn {
    private Integer checkInId;
    private Integer memberAccount;
    private String memberName;
    private Integer classOrderId;
    private Date checkInTime;
    private String checkInType;
    private String remark;
}
