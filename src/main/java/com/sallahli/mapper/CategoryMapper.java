package com.sallahli.mapper;

import com.sallahli.dto.sallahli.CategoryDTO;
import com.sallahli.model.Category;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface CategoryMapper extends Mapper<Category, CategoryDTO> {

    @Override
    @Mapping(target = "iconMedia", source = "iconMedia")
    CategoryDTO toDto(Category model);

    @Override
    @Mapping(target = "iconMedia", source = "iconMedia")
    Category toModel(CategoryDTO dto);

    @Override
    default Class<Category> getModelClass() {
        return Category.class;
    }
}

