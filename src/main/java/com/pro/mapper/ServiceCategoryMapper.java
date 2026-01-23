package com.pro.mapper;

import com.pro.dto.sallahli.ServiceCategoryDTO;
import com.pro.model.ServiceCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface ServiceCategoryMapper extends Mapper<ServiceCategory, ServiceCategoryDTO> {

    @Override
    @Mapping(target = "iconMedia", source = "iconMedia")
    ServiceCategoryDTO toDto(ServiceCategory model);

    @Override
    @Mapping(target = "iconMedia", source = "iconMedia")
    ServiceCategory toModel(ServiceCategoryDTO dto);

    @Override
    default Class<ServiceCategory> getModelClass() {
        return ServiceCategory.class;
    }
}
