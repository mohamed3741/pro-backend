package com.pro.dto.sallahli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategoryDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private MediaDTO iconMedia;
    private Boolean active;
}
