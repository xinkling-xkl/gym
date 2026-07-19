package com.gym.entity;

import java.util.Date;

public class ClassTable {
    private Integer classId;
    private String className;
    private Date classBegin;
    private String classTime;
    private String coach;

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Date getClassBegin() {
        return classBegin;
    }

    public void setClassBegin(Date classBegin) {
        this.classBegin = classBegin;
    }

    public String getClassTime() {
        return classTime;
    }

    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }
}
