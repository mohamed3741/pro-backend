package com.pro.mapper;

import com.pro.dto.sallahli.ProDTO;
import com.pro.model.Pro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ServiceCategoryMapper.class, ZoneMapper.class, MediaMapper.class})
public interface ProMapper extends Mapper<Pro, ProDTO> {

    @Override
    @Mapping(target = "trade", source = "trade")
    @Mapping(target = "baseZone", source = "baseZone")
    @Mapping(target = "cniFrontMedia", source = "cniFrontMedia")
    @Mapping(target = "cniBackMedia", source = "cniBackMedia")
    @Mapping(target = "selfieMedia", source = "selfieMedia")
    ProDTO toDto(Pro model);

    @Override
    @Mapping(target = "trade", source = "trade")
    @Mapping(target = "baseZone", source = "baseZone")
    @Mapping(target = "cniFrontMedia", source = "cniFrontMedia")
    @Mapping(target = "cniBackMedia", source = "cniBackMedia")
    @Mapping(target = "selfieMedia", source = "selfieMedia")
    Pro toModel(ProDTO dto);

    @Override
    default Class<Pro> getModelClass() {
        return Pro.class;
    }
}
