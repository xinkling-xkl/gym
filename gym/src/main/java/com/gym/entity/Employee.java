package com.gym.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class Employee {
    private Integer employeeAccount;
    private String employeeName;
    private String avatar;
    private String employeeGender;
    private Integer employeeAge;
    private String specialty;
    private String certificate;
    private String intro;
    private Date entryTime;
    private String staff;
    private String employeeMessage;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String employeePassword;
}
