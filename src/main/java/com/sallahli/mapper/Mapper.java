package com.sallahli.mapper;

import java.util.List;
import java.util.Set;

public interface Mapper<M, D> {
    M toModel(D dto);

    D toDto(M model);

    List<M> toModels(List<D> dtos);

    Set<M> toModels(Set<D> dtos);

    List<D> toDtos(List<M> models);

    Set<D> toDtos(Set<M> models);

    Class<M> getModelClass();
}


