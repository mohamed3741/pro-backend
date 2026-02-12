package com.sallahli.mapper;

import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.model.Pro;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = { CategoryMapper.class, ZoneMapper.class, MediaMapper.class,
        AdminMapper.class })
public interface ProMapper extends Mapper<Pro, ProDTO> {

    @Override
    @Mapping(target = "trade", source = "trade")
    @Mapping(target = "baseZone", source = "baseZone")
    @Mapping(target = "cniFrontMedia", source = "cniFrontMedia")
    @Mapping(target = "cniBackMedia", source = "cniBackMedia")
    @Mapping(target = "selfieMedia", source = "selfieMedia")
    @Mapping(target = "categories", source = "categories")
    ProDTO toDto(Pro model);

    @Override
    @Mapping(target = "trade", source = "trade")
    @Mapping(target = "baseZone", source = "baseZone")
    @Mapping(target = "cniFrontMedia", source = "cniFrontMedia")
    @Mapping(target = "cniBackMedia", source = "cniBackMedia")
    @Mapping(target = "selfieMedia", source = "selfieMedia")
    @Mapping(target = "categories", source = "categories")
    Pro toModel(ProDTO dto);

    @Override
    default Class<Pro> getModelClass() {
        return Pro.class;
    }
}
