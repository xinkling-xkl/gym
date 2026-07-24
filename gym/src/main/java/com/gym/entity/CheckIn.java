package com.gym.entity;

import java.util.Date;

public class CheckIn {
    private Integer checkInId;
    private Integer memberAccount;
    private String memberName;
    private Integer classOrderId;
    private Date checkInTime;
    private String checkInType;
    private String remark;

    public Integer getCheckInId() { return checkInId; }
    public void setCheckInId(Integer checkInId) { this.checkInId = checkInId; }

    public Integer getMemberAccount() { return memberAccount; }
    public void setMemberAccount(Integer memberAccount) { this.memberAccount = memberAccount; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Integer getClassOrderId() { return classOrderId; }
    public void setClassOrderId(Integer classOrderId) { this.classOrderId = classOrderId; }

    public Date getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Date checkInTime) { this.checkInTime = checkInTime; }

    public String getCheckInType() { return checkInType; }
    public void setCheckInType(String checkInType) { this.checkInType = checkInType; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
