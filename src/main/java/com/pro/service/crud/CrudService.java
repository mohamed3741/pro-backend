package com.pro.service.crud;

import java.util.List;

public interface CrudService<D> {

    List<D> findAll();

    D findById(Long id);

    D create(D dto);

    D update(Long id, D dto);

    void delete(Long id);
}


