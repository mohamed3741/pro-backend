package com.sallahli.utils;


import com.sallahli.dto.search.Criteria;
import com.sallahli.dto.search.Filter;
import com.sallahli.dto.search.MatchMode;

public class SqlUtils {


    public static String computeWhereFilters(Criteria criteria) {
        StringBuilder filters = new StringBuilder();

        if (criteria.getFilters() != null && !criteria.getFilters().isEmpty()) {
            for (Filter filter : criteria.getFilters()) {
                String alias = filter.getKey().contains(".") ? "" : "res.";
                if (MatchMode.equals.equals(filter.getMatchMode())) {
                    filters.append(" AND ")
                            .append(alias).append(filter.getKey())
                            .append(" = ").append(filter.getValue()).append(" ");
                } else if (MatchMode.in.equals(filter.getMatchMode())) {
                    filters.append(" AND ")
                            .append(alias).append(filter.getKey())
                            .append(" IN (").append(filter.getValue().replace(";", ",")).append(") ");
                }
            }
        }

        return filters.toString();
    }

    public static String computeOrderBy(Criteria criteria) {
        StringBuilder orderBy = new StringBuilder(" ORDER BY ");

        if (criteria.getSortField() != null && !criteria.getSortField().isBlank()) {
            orderBy.append("res.").append(criteria.getSortField())
                    .append(criteria.getSortOrder() != null && criteria.getSortOrder() == 1 ? " DESC" : " ASC")
                    .append(", distance ASC");
        } else {
            orderBy.append("res.opening_status_ordinal ASC, distance ASC");
        }

        return orderBy.toString();
    }
}


