package com.pro.controller.sallahli;

import com.pro.dto.sallahli.JobDTO;
import com.pro.dto.sallahli.request.RatingSubmissionRequest;
import com.pro.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sallahli/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Management", description = "APIs for managing jobs and ratings")
public class JobController {

    private final JobService jobService;

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobDTO> getJob(@PathVariable Long id) {
        JobDTO job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/pro/{proId}")
    @Operation(summary = "Get all jobs for a professional")
    public ResponseEntity<List<JobDTO>> getProJobs(@PathVariable Long proId) {
        List<JobDTO> jobs = jobService.getProJobs(proId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get all jobs for a client")
    public ResponseEntity<List<JobDTO>> getClientJobs(@PathVariable Long clientId) {
        List<JobDTO> jobs = jobService.getClientJobs(clientId);
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "Start a job")
    public ResponseEntity<JobDTO> startJob(@PathVariable Long id, @RequestParam Long proId) {
        JobDTO job = jobService.startJob(id, proId);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete a job")
    public ResponseEntity<JobDTO> completeJob(@PathVariable Long id, @RequestParam Long proId) {
        JobDTO job = jobService.completeJob(id, proId);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a job")
    public ResponseEntity<JobDTO> cancelJob(@PathVariable Long id,
                                           @RequestParam Long proId,
                                           @RequestParam String reason) {
        JobDTO job = jobService.cancelJob(id, proId, reason);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Submit rating for a completed job")
    public ResponseEntity<Void> submitRating(@PathVariable Long id,
                                           @Valid @RequestBody RatingSubmissionRequest request,
                                           @RequestParam Long clientId) {
        jobService.submitRating(request, clientId);
        return ResponseEntity.ok().build();
    }
}
