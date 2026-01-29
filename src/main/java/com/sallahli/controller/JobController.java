package com.sallahli.controller;

import com.sallahli.dto.sallahli.JobDTO;
import com.sallahli.model.Enum.JobStatus;
import com.sallahli.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Management", description = "APIs for managing jobs between clients and professionals")
public class JobController {

    private final JobService jobService;

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all jobs", description = "Returns all jobs (Admin only)")
    public ResponseEntity<List<JobDTO>> findAll() {
        log.debug("REST request to get all jobs");
        return ResponseEntity.ok(jobService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Get job by ID", description = "Returns a single job")
    public ResponseEntity<JobDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get job {}", id);
        return ResponseEntity.ok(jobService.findById(id));
    }

    // ========================================================================
    // Job Lifecycle
    // ========================================================================

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Complete job", description = "Marks job as completed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job completed successfully"),
            @ApiResponse(responseCode = "400", description = "Job cannot be completed")
    })
    public ResponseEntity<JobDTO> completeJob(@PathVariable Long id) {
        log.debug("REST request to complete job {}", id);
        return ResponseEntity.ok(jobService.completeJob(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Cancel job", description = "Cancels a job")
    public ResponseEntity<JobDTO> cancelJob(
            @PathVariable Long id,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        log.debug("REST request to cancel job {} with reason: {}", id, reason);
        return ResponseEntity.ok(jobService.cancelJob(id, reason));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Mark no-show", description = "Marks job as no-show (pro didn't arrive)")
    public ResponseEntity<JobDTO> markNoShow(@PathVariable Long id) {
        log.debug("REST request to mark job {} as no-show", id);
        return ResponseEntity.ok(jobService.markNoShow(id));
    }

    // ========================================================================
    // Query Operations
    // ========================================================================

    @GetMapping("/by-pro/{proId}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get jobs by pro", description = "Returns all jobs for a specific professional")
    public ResponseEntity<List<JobDTO>> findByProId(@PathVariable Long proId) {
        log.debug("REST request to get jobs for pro {}", proId);
        return ResponseEntity.ok(jobService.findByProId(proId));
    }

    @GetMapping("/by-client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get jobs by client", description = "Returns all jobs for a specific client")
    public ResponseEntity<List<JobDTO>> findByClientId(@PathVariable Long clientId) {
        log.debug("REST request to get jobs for client {}", clientId);
        return ResponseEntity.ok(jobService.findByClientId(clientId));
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get jobs by status", description = "Returns all jobs with a specific status")
    public ResponseEntity<List<JobDTO>> findByStatus(@PathVariable JobStatus status) {
        log.debug("REST request to get jobs with status {}", status);
        return ResponseEntity.ok(jobService.findByStatus(status));
    }

    @GetMapping("/by-pro/{proId}/active")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get active jobs for pro", description = "Returns all in-progress jobs for a professional")
    public ResponseEntity<List<JobDTO>> findActiveJobsByProId(@PathVariable Long proId) {
        log.debug("REST request to get active jobs for pro {}", proId);
        return ResponseEntity.ok(jobService.findActiveJobsByProId(proId));
    }

    @GetMapping("/completed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get completed jobs in range", description = "Returns completed jobs within date range")
    public ResponseEntity<List<JobDTO>> findCompletedJobsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.debug("REST request to get completed jobs between {} and {}", startDate, endDate);
        return ResponseEntity.ok(jobService.findCompletedJobsInDateRange(startDate, endDate));
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    @GetMapping("/by-pro/{proId}/count")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get completed job count", description = "Returns count of completed jobs for a pro")
    public ResponseEntity<Long> countCompletedJobsByPro(@PathVariable Long proId) {
        log.debug("REST request to count completed jobs for pro {}", proId);
        return ResponseEntity.ok(jobService.countCompletedJobsByPro(proId));
    }

    @GetMapping("/by-pro/{proId}/rating")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get average rating", description = "Returns average rating for a pro's jobs")
    public ResponseEntity<Double> getAverageRatingByPro(@PathVariable Long proId) {
        log.debug("REST request to get average rating for pro {}", proId);
        return ResponseEntity.ok(jobService.getAverageRatingByPro(proId));
    }
}
