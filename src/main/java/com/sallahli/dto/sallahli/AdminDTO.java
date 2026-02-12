package com.sallahli.dto.sallahli;

import com.sallahli.dto.UserDTO;
import com.sallahli.model.Enum.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminDTO extends UserDTO {

    private String profilePhoto;
    private AdminRole role;
    private String department;
    private Boolean isActive;
    private Boolean archived;
    private LocalDateTime lastLoginAt;
}
