package com.sallahli.mapper;

import com.sallahli.dto.appconfig.AppConfigBundleDto;
import com.sallahli.dto.appconfig.CreateConfigRequest;
import com.sallahli.model.AppConfigBundle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppConfigBundleMapper {

    AppConfigBundleDto toDto(AppConfigBundle entity);

    AppConfigBundle toEntity(AppConfigBundleDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hash", ignore = true)
    AppConfigBundle toEntity(CreateConfigRequest request, String createdBy);
}

