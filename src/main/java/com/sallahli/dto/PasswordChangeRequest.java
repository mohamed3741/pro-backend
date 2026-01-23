package com.sallahli.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PasswordChangeRequest {
    private String oldPassword;
    private String newPassword;
}
