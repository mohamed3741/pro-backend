package com.pro.dto.search;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class PaginatedResult<T> {
    private Long totalRecords;
    private List<T> data;

}


