package com.pro.model;

import com.pro.utils.HasTimestamps;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@MappedSuperclass
public class User extends HasTimestamps {
    private String tel;
    private String username;
    private String email;
    private String firstName;
    private String lastName ;
}


