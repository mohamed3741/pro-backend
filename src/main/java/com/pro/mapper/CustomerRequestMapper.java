package com.pro.mapper;

import com.pro.dto.sallahli.CustomerRequestDTO;
import com.pro.model.CustomerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ClientMapper.class, ServiceCategoryMapper.class, MediaMapper.class})
public interface CustomerRequestMapper extends Mapper<CustomerRequest, CustomerRequestDTO> {

    @Override
    @Mapping(target = "client", source = "client")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "voiceNoteMedia", source = "voiceNoteMedia")
    CustomerRequestDTO toDto(CustomerRequest model);

    @Override
    @Mapping(target = "client", source = "client")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "voiceNoteMedia", source = "voiceNoteMedia")
    CustomerRequest toModel(CustomerRequestDTO dto);

    @Override
    default Class<CustomerRequest> getModelClass() {
        return CustomerRequest.class;
    }
}
