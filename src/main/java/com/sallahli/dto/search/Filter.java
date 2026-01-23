package com.sallahli.dto.search;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Filter {
    private String key;
    private String value;
    private MatchMode matchMode;
}


