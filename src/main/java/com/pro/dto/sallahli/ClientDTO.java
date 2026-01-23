package com.pro.dto.sallahli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Long id;
    private String tel;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private MediaDTO logo;
    private Long userId;
    private String customerId;
    private Boolean archived;
    private Integer wallet;
    private Boolean ccOnly;
    private Boolean isDeleted;
    private Boolean isActive;
    private Boolean isTelVerified;
    private String nationality;
    private String nationalityCode;
    private LocalDate birthDate;
    private Boolean guest;
    private String gender;
    private Boolean adsAccepted;
    private Long referralCode;
    private Long referredBy;
    private Long referralCounter;
    private String loginProvider;
}
