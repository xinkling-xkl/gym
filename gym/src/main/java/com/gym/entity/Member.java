package com.gym.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Member {
    private Integer memberAccount;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String memberPassword;
    private String memberName;
    private String memberGender;
    private Integer memberAge;
    private Double memberHeight;
    private Double memberWeight;
    private Long memberPhone;
    private LocalDate cardTime;
    private Integer cardClass;
    private Integer cardNextClass;
    private LocalDate cardExpireDate;
    private String avatar;
}
