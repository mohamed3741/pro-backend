package com.sallahli.controller;

import com.sallahli.dto.sallahli.request.RatingSubmissionRequest;
import com.sallahli.model.Job;
import com.sallahli.model.Rating;
import com.sallahli.service.JobService;
import com.sallahli.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Tag(name = "Rating", description = "Rating management APIs")
public class RatingController {

    private final RatingService ratingService;
    private final JobService jobService;

    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasAnyAuthority('CLIENT', 'PRO')")
    @Operation(summary = "Submit a rating for a job")
    public ResponseEntity<Rating> createRating(@PathVariable Long jobId, @RequestBody RatingSubmissionRequest request) {
        Job job = jobService.getEntityById(jobId);
        return ResponseEntity.ok(ratingService.createRating(job, request));
    }

    @GetMapping("/pro/{proId}/average")
    @Operation(summary = "Get average rating for a pro")
    public ResponseEntity<Double> getProAverageRating(@PathVariable Long proId) {
        return ResponseEntity.ok(ratingService.getProAverageRating(proId));
    }

    @GetMapping("/pro/{proId}/count")
    @Operation(summary = "Get rating count for a pro")
    public ResponseEntity<Long> getProRatingCount(@PathVariable Long proId) {
        return ResponseEntity.ok(ratingService.getProRatingCount(proId));
    }
}
