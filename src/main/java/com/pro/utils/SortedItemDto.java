package com.pro.utils;

import lombok.Data;

import java.io.Serial;


@Data
public abstract class SortedItemDto extends HasTimestampsDTO {
    @Serial
    private static final long serialVersionUID = 1189049497002892362L;
    private Long sortId;
}


