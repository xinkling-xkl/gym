package com.gym.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Employee {
    private Integer employeeAccount;
    private String employeeName;
    private String employeeGender;
    private Integer employeeAge;
    private Date entryTime;
    private String staff;
    private String employeeMessage;
    private String employeePassword;
}
