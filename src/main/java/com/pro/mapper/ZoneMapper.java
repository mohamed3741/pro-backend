package com.pro.mapper;

import com.pro.dto.sallahli.ZoneDTO;
import com.pro.model.Zone;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
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
