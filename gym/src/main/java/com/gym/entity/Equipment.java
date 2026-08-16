package com.gym.entity;

import lombok.Data;

@Data
public class Equipment {
    private Integer equipmentId;
    private String equipmentName;
    private String equipmentLocation;
    private String equipmentStatus;
    private String equipmentMessage;
}
