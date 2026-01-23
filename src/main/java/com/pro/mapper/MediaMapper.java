package com.pro.mapper;

import com.pro.model.Media;
import com.pro.dto.MediaDTO;
import org.mapstruct.Builder;

@org.mapstruct.Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MediaMapper extends Mapper<Media, MediaDTO> {

    @Override
    default Class<Media> getModelClass() {
        return Media.class;
    }
}



