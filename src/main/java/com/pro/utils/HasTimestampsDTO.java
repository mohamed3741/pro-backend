package com.pro.utils;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HasTimestampsDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 445581983005788860L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


