package com.sallahli.dto.sallahli;

import com.sallahli.dto.MediaDTO;
import com.sallahli.model.Enum.LeadType;
import com.sallahli.model.Enum.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;
    private String code;
    private String name;
    private String nameAr;
    private String description;
    private String descriptionAr;
    private MediaDTO iconMedia;
    private LeadType leadType;
    private BigDecimal leadCost;
    private Integer matchLimit;
    private WorkflowType workflowType;
    private Boolean active;
}
