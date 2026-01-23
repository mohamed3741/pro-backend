package com.pro.service.generic;

import com.pro.repository.generic.GenericRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class GenericService<T>  {

    @Autowired
    protected GenericRepository<T> genericRepository;

    public List<T> findAll(){
            return genericRepository.findAll();
    }

    public T save(T entity) {
            return genericRepository.save(entity);
    }

    public List<T> savAll(List<T> entities){
        return genericRepository.saveAll(entities);
    }

    public T findById(Long id){
        return genericRepository.findById(id).orElseThrow(() -> new RuntimeException("not found"));
    }

    public void delete(Long id){
            genericRepository.deleteById(id);
    }
}


