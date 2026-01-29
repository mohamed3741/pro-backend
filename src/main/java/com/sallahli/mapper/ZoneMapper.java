package com.sallahli.mapper;

import com.sallahli.dto.sallahli.ZoneDTO;
import com.sallahli.model.Zone;

@org.mapstruct.Mapper(componentModel = "spring")
public interface ZoneMapper extends Mapper<Zone, ZoneDTO> {

    @Override
    ZoneDTO toDto(Zone model);

    @Override
    Zone toModel(ZoneDTO dto);

    @Override
    default Class<Zone> getModelClass() {
        return Zone.class;
    }
}

