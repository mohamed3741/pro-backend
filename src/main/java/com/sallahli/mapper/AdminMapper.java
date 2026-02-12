package com.sallahli.mapper;

import com.sallahli.dto.sallahli.AdminDTO;
import com.sallahli.model.Admin;
import org.mapstruct.Builder;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@org.mapstruct.Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AdminMapper extends Mapper<Admin, AdminDTO> {

    @Override
    Admin toModel(AdminDTO dto);

    @Override
    @Named("toDto")
    AdminDTO toDto(Admin admin);

    @Named("toLightDto")
    AdminDTO toLightDto(Admin admin);

    @Override
    default List<AdminDTO> toDtos(List<Admin> models) {
        if (models == null) {
            return Collections.emptyList();
        }
        return models.stream()
                .map(this::toLightDto)
                .collect(Collectors.toList());
    }

    @Override
    default Class<Admin> getModelClass() {
        return Admin.class;
    }
}
