package com.sallahli.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}

