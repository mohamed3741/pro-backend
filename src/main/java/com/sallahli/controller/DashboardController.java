package com.sallahli.controller;

import com.sallahli.dto.sallahli.CustomerRequestDTO;
import com.sallahli.dto.sallahli.DashboardStatsDTO;
import com.sallahli.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Dashboard", description = "APIs for managing admin dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard stats", description = "Returns global KPIs for the admin dashboard")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        log.info("Fetching dashboard stats");
        DashboardStatsDTO stats = dashboardService.getStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get recent requests", description = "Returns a list of recent customer requests")
    public ResponseEntity<List<CustomerRequestDTO>> getRecentRequests(
            @RequestParam(name = "limit", required = false, defaultValue = "5") int limit) {
        log.info("Fetching recent requests with limit: {}", limit);
        List<CustomerRequestDTO> requests = dashboardService.getRecentRequests(limit);
        return ResponseEntity.ok(requests);
    }
}
