package com.sallahli.utils;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@MappedSuperclass
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class SortedItem extends HasTimestamps{
    @Serial
    private static final long serialVersionUID = 7018117102063074524L;
    @GeneratedValue
    private Long sortId;
}


