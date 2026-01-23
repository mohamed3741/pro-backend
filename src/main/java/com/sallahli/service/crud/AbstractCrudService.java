package com.sallahli.service.crud;

import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.Mapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractCrudService<M, D> implements CrudService<D> {

    private final org.springframework.data.jpa.repository.JpaRepository<M, Long> repository;
    private final Mapper<M, D> mapper;

    protected AbstractCrudService(org.springframework.data.jpa.repository.JpaRepository<M, Long> repository,
                                  Mapper<M, D> mapper) {
        Assert.notNull(repository, "Repository must not be null");
        Assert.notNull(mapper, "Mapper must not be null");
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<D> findAll() {
        return mapper.toDtos(repository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public D findById(Long id) {
        return mapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public D create(D dto) {
        Assert.notNull(dto, "DTO must not be null");
        M entity = mapper.toModel(dto);
        applyRelationships(entity, dto);
        beforePersist(entity, dto, true);
        M saved = repository.save(entity);
        D response = mapper.toDto(saved);
        afterPersist(saved, dto, response, true);
        return response;
    }

    @Override
    @Transactional
    public D update(Long id, D dto) {
        Assert.notNull(dto, "DTO must not be null");
        M entity = findEntity(id);
        M mapped = mapper.toModel(dto);
        copyNonNullProperties(mapped, entity);
        applyRelationships(entity, dto);
        beforePersist(entity, dto, false);
        M saved = repository.save(entity);
        D response = mapper.toDto(saved);
        afterPersist(saved, dto, response, false);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Resource not found with id: " + id);
        }
        repository.deleteById(id);
    }

    protected M findEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Resource not found with id: " + id));
    }

    protected void applyRelationships(M entity, D dto) {
        // default no-op
    }

    protected void beforePersist(M entity, D dto, boolean isNew) {
        // hook for subclasses
    }

    protected void afterPersist(M entity, D requestDto, D responseDto, boolean isNew) {
        // hook for subclasses
    }

    protected Mapper<M, D> getMapper() {
        return mapper;
    }

    protected org.springframework.data.jpa.repository.JpaRepository<M, Long> getRepository() {
        return repository;
    }

    protected String[] getIgnoredProperties() {
        return new String[]{"id", "createdAt", "updatedAt"};
    }

    protected void copyNonNullProperties(M source, M target) {
        if (source == null || target == null) {
            return;
        }
        Set<String> ignored = new HashSet<>(Arrays.asList(getIgnoredProperties()));
        BeanWrapper src = new BeanWrapperImpl(source);
        BeanWrapper trg = new BeanWrapperImpl(target);

        for (PropertyDescriptor descriptor : src.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            if ("class".equals(propertyName) || ignored.contains(propertyName)) {
                continue;
            }
            Object value = src.getPropertyValue(propertyName);
            if (value != null) {
                trg.setPropertyValue(propertyName, value);
            }
        }
    }
}


