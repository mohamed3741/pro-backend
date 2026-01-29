package com.sallahli.mapper;

import com.sallahli.dto.sallahli.RatingDTO;
import com.sallahli.model.Rating;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = {JobMapper.class, CustomerRequestMapper.class, ClientMapper.class, ProMapper.class})
public interface RatingMapper extends Mapper<Rating, RatingDTO> {

    @Override
    @Mapping(target = "job", source = "job")
    @Mapping(target = "request", source = "request")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "pro", source = "pro")
    RatingDTO toDto(Rating model);

    @Override
    @Mapping(target = "job", source = "job")
    @Mapping(target = "request", source = "request")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "pro", source = "pro")
    Rating toModel(RatingDTO dto);

    @Override
    default Class<Rating> getModelClass() {
        return Rating.class;
    }
}

