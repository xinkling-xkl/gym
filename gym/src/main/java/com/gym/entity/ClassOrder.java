package com.gym.entity;

import java.util.Date;

public class ClassOrder {
    private Integer classOrderId;
    private Integer classId;
    private String coach;
    private String memberName;
    private String memberAccount;
    private Date classBegin;

    public Integer getClassOrderId() {
        return classOrderId;
    }

    public void setClassOrderId(Integer classOrderId) {
        this.classOrderId = classOrderId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberAccount() {
        return memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
    }

    public Date getClassBegin() {
        return classBegin;
    }

    public void setClassBegin(Date classBegin) {
        this.classBegin = classBegin;
    }
}
