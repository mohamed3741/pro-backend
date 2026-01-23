package com.sallahli.utils;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SortItemRepository<T> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {



}


