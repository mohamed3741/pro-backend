package com.pro.dto.search;
import lombok.Data;
import lombok.ToString;

import java.util.List;
@Data
@ToString(onlyExplicitlyIncluded = true)
public class Criteria {
    private Integer page;
    private Integer pageSize;
    private List<Filter> filters;
    private List<List<Filter>> orFilters;
    private String sortField;
    private Integer sortOrder;
}


