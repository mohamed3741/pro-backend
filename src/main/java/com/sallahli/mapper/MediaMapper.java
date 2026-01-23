package com.sallahli.mapper;

import com.sallahli.model.Media;
import com.sallahli.dto.MediaDTO;
import org.mapstruct.Builder;

@org.mapstruct.Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MediaMapper extends Mapper<Media, MediaDTO> {

    @Override
    default Class<Media> getModelClass() {
        return Media.class;
    }
}



