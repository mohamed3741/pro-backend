package com.sallahli.dto.search;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class SallahliSpecification<T> implements Specification<T> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final List<Filter> filters;

    public SallahliSpecification(List<Filter> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (filters.isEmpty()) {
            return criteriaBuilder.conjunction(); // Always true predicate
        }

        List<Predicate> predicates = new ArrayList<>();

        for (Filter filter : filters) {
            if (isValidFilter(filter)) {
                try {
                    Predicate predicate = getPredicate(filter, criteriaBuilder, root);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                } catch (Exception e) {
                    log.warn("Error processing filter {}: {}", filter.getKey(), e.getMessage());
                    // Continue processing other filters instead of failing completely
                }
            }
        }

        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction(); // No valid predicates, return true
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private boolean isValidFilter(Filter filter) {
        return filter != null &&
                filter.getKey() != null &&
                !filter.getKey().trim().isEmpty() &&
                filter.getMatchMode() != null &&
                (filter.getValue() != null || filter.getMatchMode() == MatchMode.isNull);
    }

    private Predicate getPredicate(Filter filter, CriteriaBuilder criteriaBuilder, Root<T> root) {
        try {
            Path<?> path = getPath(filter, root);
            if (path == null) {
                log.warn("Unable to resolve path for filter key: {}", filter.getKey());
                return null;
            }

            return buildPredicateByMatchMode(filter, criteriaBuilder, path);

        } catch (Exception e) {
            log.error("Error building predicate for filter {}: {}", filter.getKey(), e.getMessage());
            return null;
        }
    }

    private Predicate buildPredicateByMatchMode(Filter filter, CriteriaBuilder criteriaBuilder, Path<?> path) {
        switch (filter.getMatchMode()) {
            case startsWith:
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        filter.getValue().toLowerCase() + "%");
            case isNull:
                return path.isNull();
            case isNotNull:
                return path.isNotNull();
            case contains:
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        "%" + filter.getValue().toLowerCase() + "%");
            case notContains:
                return criteriaBuilder.notLike(
                        criteriaBuilder.lower(path.as(String.class)),
                        "%" + filter.getValue().toLowerCase() + "%");
            case endsWith:
                return criteriaBuilder.like(
                        criteriaBuilder.lower(path.as(String.class)),
                        "%" + filter.getValue().toLowerCase());
            case equals:
                return handleEqualsComparison(criteriaBuilder, path, filter.getValue());
            case notEquals:
                return criteriaBuilder.notEqual(path, parseValue(path, filter.getValue()));
            case in:
                return handleInComparison(path, filter.getValue());
            case gt:
                return handleGreaterThan(criteriaBuilder, path, filter.getValue());
            case lt:
                return handleLessThan(criteriaBuilder, path, filter.getValue());
            case gte:
                return handleGreaterThanOrEqual(criteriaBuilder, path, filter.getValue());
            case lte:
                return handleLessThanOrEqual(criteriaBuilder, path, filter.getValue());
            case dateIs:
                return handleDateIs(criteriaBuilder, path, filter.getValue());
            case dateAfter:
                return handleDateAfter(criteriaBuilder, path, filter.getValue());
            case dateBefore:
                return handleDateBefore(criteriaBuilder, path, filter.getValue());
            case between:
                return handleBetweenComparison(criteriaBuilder, path, filter.getValue());
            default:
                log.warn("Unsupported match mode: {}", filter.getMatchMode());
                return null;
        }
    }

    private Predicate handleEqualsComparison(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        if (path.getJavaType() == String.class) {
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(path.as(String.class)),
                    value.toLowerCase());
        }
        return criteriaBuilder.equal(path, parseValue(path, value));
    }

    private Predicate handleInComparison(Path<?> path, String value) {
        List<String> stringValues = Arrays.asList(value.split(";"));

        if (path.getJavaType().isEnum()) {
            List<Enum<?>> enumValues = stringValues.stream()
                    .map(v -> parseEnumValue(path.getJavaType(), v))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return enumValues.isEmpty() ? null : path.in(enumValues);

        } else if (path.getJavaType() == Long.class || path.getJavaType() == long.class) {
            List<Long> values = stringValues.stream()
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return values.isEmpty() ? null : path.in(values);

        } else {
            return path.in(stringValues);
        }
    }

    private Predicate handleDateIs(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            return criteriaBuilder.between(
                    path.as(LocalDateTime.class),
                    date.atStartOfDay(),
                    date.atTime(23, 59, 59, 999_999_999));
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format for dateIs filter: {}", value);
            return null;
        }
    }

    private Predicate handleDateAfter(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            LocalDateTime dateTime;

            if (value.length() == 12) {
                // Format: yyyyMMddHHmm (e.g., "202511111213" = 2025-11-11 12:13)
                dateTime = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } else if (value.length() == 8) {
                // Format: yyyyMMdd (e.g., "20251111" = 2025-11-11, use start of day)
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                dateTime = date.atStartOfDay();
            } else {
                log.warn("Invalid date format for dateAfter filter: {}", value);
                return null;
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    path.as(LocalDateTime.class),
                    dateTime);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format for dateAfter filter: {}", value, e);
            return null;
        }
    }

    private Predicate handleDateBefore(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            LocalDateTime dateTime;

            if (value.length() == 12) {
                // Format: yyyyMMddHHmm (e.g., "202511111213" = 2025-11-11 12:13)
                dateTime = LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } else if (value.length() == 8) {
                // Format: yyyyMMdd (e.g., "20251111" = 2025-11-11, use end of day)
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                dateTime = date.atTime(23, 59, 59, 999_999_999);
            } else {
                log.warn("Invalid date format for dateBefore filter: {}", value);
                return null;
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    path.as(LocalDateTime.class),
                    dateTime);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format for dateBefore filter: {}", value, e);
            return null;
        }
    }

    private Predicate handleBetweenComparison(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        String[] elements = value.split(";");
        if (elements.length != 2) {
            log.warn("Between filter requires exactly 2 values separated by ';', got: {}", value);
            return null;
        }

        try {
            Class<?> javaType = path.getJavaType();

            if (javaType == LocalDateTime.class) {
                LocalDateTime from = LocalDate.parse(elements[0], DATE_FORMATTER).atStartOfDay();
                LocalDateTime to = LocalDate.parse(elements[1], DATE_FORMATTER).atTime(23, 59, 59, 999_999_999);
                return criteriaBuilder.between(path.as(LocalDateTime.class), from, to);

            } else if (javaType == LocalDate.class) {
                LocalDate from = LocalDate.parse(elements[0], DATE_FORMATTER);
                LocalDate to = LocalDate.parse(elements[1], DATE_FORMATTER);
                return criteriaBuilder.between(path.as(LocalDate.class), from, to);

            } else if (javaType == Double.class || javaType == double.class) {
                Double from = Double.parseDouble(elements[0]);
                Double to = Double.parseDouble(elements[1]);
                return criteriaBuilder.between(path.as(Double.class), from, to);

            } else if (javaType == Long.class || javaType == long.class) {
                Long from = Long.parseLong(elements[0]);
                Long to = Long.parseLong(elements[1]);
                return criteriaBuilder.between(path.as(Long.class), from, to);

            } else {
                return criteriaBuilder.between(path.as(String.class), elements[0], elements[1]);
            }
        } catch (Exception e) {
            log.warn("Error parsing between values '{}': {}", value, e.getMessage());
            return null;
        }
    }

    private Path<?> getPath(Filter filter, Root<T> root) {
        try {
            List<String> nestedFields = Arrays.asList(filter.getKey().split("\\."));

            if (nestedFields.size() == 1) {
                return root.get(filter.getKey());
            }

            Join<?, ?> join = null;
            Path<?> path = root;

            for (int i = 0; i < nestedFields.size(); i++) {
                String field = nestedFields.get(i);

                if (i == nestedFields.size() - 1) {
                    // Last field, get the actual property
                    return path.get(field);
                } else {
                    // Intermediate field, create join
                    if (join == null) {
                        join = root.join(field, JoinType.LEFT);
                        path = join;
                    } else {
                        join = join.join(field, JoinType.LEFT);
                        path = join;
                    }
                }
            }

            return path;

        } catch (Exception e) {
            log.error("Error resolving path for key '{}': {}", filter.getKey(), e.getMessage());
            return null;
        }
    }

    private Enum<?> parseEnumValue(Class<?> enumType, String value) {
        try {
            return Enum.valueOf((Class<Enum>) enumType, value);
        } catch (Exception e) {
            log.warn("Invalid enum value '{}' for type {}", value, enumType.getSimpleName());
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value: {}", value);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate handleGreaterThan(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            Object parsedValue = parseValue(path, value);
            if (parsedValue == null) {
                log.warn("Parsed value is null for gt comparison, field: {}, value: {}", path.getAlias(), value);
                return null;
            }

            Class<?> javaType = path.getJavaType();

            if (javaType == Integer.class || javaType == int.class) {
                return criteriaBuilder.greaterThan((Path<Integer>) path, (Integer) parsedValue);
            }
            if (javaType == Long.class || javaType == long.class) {
                return criteriaBuilder.greaterThan((Path<Long>) path, (Long) parsedValue);
            }
            if (javaType == Double.class || javaType == double.class) {
                return criteriaBuilder.greaterThan((Path<Double>) path, (Double) parsedValue);
            }
            if (javaType == Float.class || javaType == float.class) {
                return criteriaBuilder.greaterThan((Path<Float>) path, (Float) parsedValue);
            }
            if (javaType == LocalDateTime.class) {
                LocalDateTime dateValue = parseLocalDateTime(value);
                return dateValue != null ? criteriaBuilder.greaterThan((Path<LocalDateTime>) path, dateValue) : null;
            }
            if (javaType == LocalDate.class) {
                LocalDate dateValue = parseLocalDate(value);
                return dateValue != null ? criteriaBuilder.greaterThan((Path<LocalDate>) path, dateValue) : null;
            }
            if (javaType == String.class) {
                return criteriaBuilder.greaterThan((Path<String>) path, (String) parsedValue);
            }
            if (Comparable.class.isAssignableFrom(javaType) && parsedValue instanceof Comparable) {
                return criteriaBuilder.greaterThan((Path<Comparable>) path, (Comparable) parsedValue);
            }

            log.warn("Unsupported type for gt comparison: {}", javaType.getSimpleName());
            return null;

        } catch (Exception e) {
            log.error("Error in handleGreaterThan for field: {}, value: {}", path.getAlias(), value, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate handleLessThan(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            Object parsedValue = parseValue(path, value);
            if (parsedValue == null) {
                log.warn("Parsed value is null for lt comparison, field: {}, value: {}", path.getAlias(), value);
                return null;
            }

            Class<?> javaType = path.getJavaType();

            if (javaType == Integer.class || javaType == int.class) {
                return criteriaBuilder.lessThan((Path<Integer>) path, (Integer) parsedValue);
            }
            if (javaType == Long.class || javaType == long.class) {
                return criteriaBuilder.lessThan((Path<Long>) path, (Long) parsedValue);
            }
            if (javaType == Double.class || javaType == double.class) {
                return criteriaBuilder.lessThan((Path<Double>) path, (Double) parsedValue);
            }
            if (javaType == Float.class || javaType == float.class) {
                return criteriaBuilder.lessThan((Path<Float>) path, (Float) parsedValue);
            }
            if (javaType == LocalDateTime.class) {
                LocalDateTime dateValue = parseLocalDateTime(value);
                return dateValue != null ? criteriaBuilder.lessThan((Path<LocalDateTime>) path, dateValue) : null;
            }
            if (javaType == LocalDate.class) {
                LocalDate dateValue = parseLocalDate(value);
                return dateValue != null ? criteriaBuilder.lessThan((Path<LocalDate>) path, dateValue) : null;
            }
            if (javaType == String.class) {
                return criteriaBuilder.lessThan((Path<String>) path, (String) parsedValue);
            }
            if (Comparable.class.isAssignableFrom(javaType) && parsedValue instanceof Comparable) {
                return criteriaBuilder.lessThan((Path<Comparable>) path, (Comparable) parsedValue);
            }

            log.warn("Unsupported type for lt comparison: {}", javaType.getSimpleName());
            return null;

        } catch (Exception e) {
            log.error("Error in handleLessThan for field: {}, value: {}", path.getAlias(), value, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate handleGreaterThanOrEqual(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            Object parsedValue = parseValue(path, value);
            if (parsedValue == null) {
                return null;
            }

            Class<?> javaType = path.getJavaType();

            if (javaType == Integer.class || javaType == int.class) {
                return criteriaBuilder.greaterThanOrEqualTo((Path<Integer>) path, (Integer) parsedValue);
            }
            if (javaType == Long.class || javaType == long.class) {
                return criteriaBuilder.greaterThanOrEqualTo((Path<Long>) path, (Long) parsedValue);
            }
            if (javaType == Double.class || javaType == double.class) {
                return criteriaBuilder.greaterThanOrEqualTo((Path<Double>) path, (Double) parsedValue);
            }
            if (javaType == LocalDateTime.class) {
                LocalDateTime dateValue = parseLocalDateTime(value);
                return dateValue != null ? criteriaBuilder.greaterThanOrEqualTo((Path<LocalDateTime>) path, dateValue)
                        : null;
            }
            if (javaType == LocalDate.class) {
                LocalDate dateValue = parseLocalDate(value);
                return dateValue != null ? criteriaBuilder.greaterThanOrEqualTo((Path<LocalDate>) path, dateValue)
                        : null;
            }
            if (Comparable.class.isAssignableFrom(javaType) && parsedValue instanceof Comparable) {
                return criteriaBuilder.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) parsedValue);
            }

            return null;
        } catch (Exception e) {
            log.error("Error in handleGreaterThanOrEqual: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate handleLessThanOrEqual(CriteriaBuilder criteriaBuilder, Path<?> path, String value) {
        try {
            Object parsedValue = parseValue(path, value);
            if (parsedValue == null) {
                return null;
            }

            Class<?> javaType = path.getJavaType();

            if (javaType == Integer.class || javaType == int.class) {
                return criteriaBuilder.lessThanOrEqualTo((Path<Integer>) path, (Integer) parsedValue);
            }
            if (javaType == Long.class || javaType == long.class) {
                return criteriaBuilder.lessThanOrEqualTo((Path<Long>) path, (Long) parsedValue);
            }
            if (javaType == Double.class || javaType == double.class) {
                return criteriaBuilder.lessThanOrEqualTo((Path<Double>) path, (Double) parsedValue);
            }
            if (javaType == LocalDateTime.class) {
                LocalDateTime dateValue = parseLocalDateTime(value);
                return dateValue != null ? criteriaBuilder.lessThanOrEqualTo((Path<LocalDateTime>) path, dateValue)
                        : null;
            }
            if (javaType == LocalDate.class) {
                LocalDate dateValue = parseLocalDate(value);
                return dateValue != null ? criteriaBuilder.lessThanOrEqualTo((Path<LocalDate>) path, dateValue) : null;
            }
            if (Comparable.class.isAssignableFrom(javaType) && parsedValue instanceof Comparable) {
                return criteriaBuilder.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) parsedValue);
            }

            return null;
        } catch (Exception e) {
            log.error("Error in handleLessThanOrEqual: {}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(String value) {
        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse LocalDateTime from value: {}", value);
                return null;
            }
        }
    }

    private LocalDate parseLocalDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse LocalDate from value: {}", value);
                return null;
            }
        }
    }

    private Object parseValue(Path<?> path, String value) {
        try {
            Class<?> javaType = path.getJavaType();

            if (javaType == String.class)
                return value;
            if (javaType == Long.class || javaType == long.class)
                return Long.parseLong(value);
            if (javaType == Integer.class || javaType == int.class)
                return Integer.parseInt(value);
            if (javaType == Double.class || javaType == double.class)
                return Double.parseDouble(value);
            if (javaType == Float.class || javaType == float.class)
                return Float.parseFloat(value);
            if (javaType == Boolean.class || javaType == boolean.class)
                return Boolean.parseBoolean(value);
            if (javaType.isEnum())
                return parseEnumValue(javaType, value);

            return value;
        } catch (Exception e) {
            log.warn("Error parsing value '{}' for type {}: {}", value, path.getJavaType(), e.getMessage());
            return value;
        }
    }
}
