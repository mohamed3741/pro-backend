package com.sallahli.dto.search;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public interface PaginatedSearchService<S,T> {

          default Page<S> findByCriteria(Criteria criteria){
            // Main AND filters
            Specification<T> specification = null;
            if (criteria.getFilters() != null && !criteria.getFilters().isEmpty()) {
                specification = new SallahliSpecification<>(criteria.getFilters());

            } else {
                System.out.println("No main filters");
            }

            // OR filter groups
            if (criteria.getOrFilters() != null && !criteria.getOrFilters().isEmpty()) {

                List<Specification<T>> orSpecs = new ArrayList<>();

                for (int groupIndex = 0; groupIndex < criteria.getOrFilters().size(); groupIndex++) {
                    List<Filter> orFilterGroup = criteria.getOrFilters().get(groupIndex);
                    if (orFilterGroup != null && !orFilterGroup.isEmpty()) {
                        Specification<T> orGroupSpec = new SallahliSpecification<>(orFilterGroup);
                        orSpecs.add(orGroupSpec);
                    }
                }

                if (!orSpecs.isEmpty()) {
                    Specification<T> orSpecification = orSpecs.get(0);
                    // Add remaining And groups with or logic
                    for (int i = 1; i < orSpecs.size(); i++) {
                        orSpecification = orSpecification.or(orSpecs.get(i));
                    }

                    // Combine with main specification
                    if (specification != null) {
                        specification = specification.and(orSpecification);
                    } else {
                        specification = orSpecification;
                    }
                }
            } else {
                System.out.println("No OR filters");
            }

            // Default specification
            if (specification == null) {
                specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
            }

            // Sorting
            String sortField = criteria.getSortField();
            Sort sort = Sort.unsorted();
            if (sortField != null) {
                sort = criteria.getSortOrder() != null && criteria.getSortOrder() == 1
                        ? Sort.by(sortField).descending()
                        : Sort.by(sortField).ascending();
            }

            // Pagination
            Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getPageSize(), sort);

            Page<T> entities = findBySpecification(specification, pageable);

            List<S> dtos = mapData(entities.getContent());

            return new PageImpl<>(dtos, pageable, entities.getTotalElements());
        }



    Page<T> findBySpecification(Specification<T> specification, Pageable pageable);

    List<S> mapData(List<T> data);
}



