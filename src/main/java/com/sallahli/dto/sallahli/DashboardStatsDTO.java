package com.sallahli.dto.sallahli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private Long totalCustomers;
    private Long totalProviders;
    private Long totalRequests;
    private BigDecimal totalRevenue;
    private Long pendingRequests;
    private Long completedRequests;
    private Long newCustomersThisMonth;
}
