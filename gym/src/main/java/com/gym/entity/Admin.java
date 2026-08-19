package com.gym.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Admin {
    private Integer adminAccount;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String adminPassword;
}
