package com.pro.service;

import com.pro.dto.sallahli.ServiceCategoryDTO;
import com.pro.mapper.ServiceCategoryMapper;
import com.pro.model.ServiceCategory;
import com.pro.repository.ServiceCategoryRepository;
import com.pro.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ServiceCategoryService extends AbstractCrudService<ServiceCategory, ServiceCategoryDTO> {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceCategoryMapper serviceCategoryMapper;

    public ServiceCategoryService(ServiceCategoryRepository repository, ServiceCategoryMapper mapper) {
        super(repository, mapper);
        this.serviceCategoryRepository = repository;
        this.serviceCategoryMapper = mapper;
    }

    public List<ServiceCategoryDTO> getActiveCategories() {
        List<ServiceCategory> categories = serviceCategoryRepository.findActiveCategoriesOrderedByName();
        return serviceCategoryMapper.toDtos(categories);
    }

    public ServiceCategoryDTO findByCode(String code) {
        ServiceCategory category = serviceCategoryRepository.findByCode(code)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Service category not found with code: " + code));
        return serviceCategoryMapper.toDto(category);
    }
}
