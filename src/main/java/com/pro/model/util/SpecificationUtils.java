package com.pro.model.util;

import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtils {
    private SpecificationUtils() {}

    public static <T> Specification<T> likeIgnoreCase(String attr, String value) {
        if (value == null || value.isBlank()) return (root, q, cb) -> cb.conjunction();
        String pattern = "%" + value.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get(attr)), pattern);
    }
}


