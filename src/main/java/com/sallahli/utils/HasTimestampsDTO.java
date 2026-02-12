package com.sallahli.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HasTimestampsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 445581983005788860L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
