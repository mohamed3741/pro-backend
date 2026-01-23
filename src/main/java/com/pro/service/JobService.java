package com.pro.service;

import com.pro.dto.sallahli.JobDTO;
import com.pro.dto.sallahli.request.RatingSubmissionRequest;
import com.pro.mapper.JobMapper;
import com.pro.model.*;
import com.pro.model.Enum.JobStatus;
import com.pro.model.Enum.RequestStatus;
import com.pro.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final RatingService ratingService;
    private final CustomerRequestService customerRequestService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<JobDTO> getProJobs(Long proId) {
        List<Job> jobs = jobRepository.findByProIdOrderByCreatedAtDesc(proId);
        return jobMapper.toDtos(jobs);
    }

    @Transactional(readOnly = true)
    public List<JobDTO> getClientJobs(Long clientId) {
        List<Job> jobs = jobRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return jobMapper.toDtos(jobs);
    }

    @Transactional(readOnly = true)
    public JobDTO getJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Job not found"));
        return jobMapper.toDto(job);
    }

    @Transactional
    public JobDTO startJob(Long jobId, Long proId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Job not found"));

        if (!job.getPro().getId().equals(proId)) {
            throw new com.pro.exceptions.AccessDeniedException("Pro can only start their own jobs");
        }

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new com.pro.exceptions.BadRequestException("Job is not in progress status");
        }

        job.setStartedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);

        // Notify client
        notificationService.sendJobStartedNotification(job);

        log.info("Job started: {}", jobId);
        return jobMapper.toDto(savedJob);
    }

    @Transactional
    public JobDTO completeJob(Long jobId, Long proId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Job not found"));

        if (!job.getPro().getId().equals(proId)) {
            throw new com.pro.exceptions.AccessDeniedException("Pro can only complete their own jobs");
        }

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new com.pro.exceptions.BadRequestException("Job must be in progress to complete");
        }

        job.setStatus(JobStatus.DONE);
        job.setDoneAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);

        // Update request status
        customerRequestService.updateRequestStatus(job.getRequest().getId(), RequestStatus.DONE);

        // Update pro stats
        updateProStats(job.getPro().getId());

        // Notify client that job is completed
        notificationService.sendJobCompletedNotification(job);

        log.info("Job completed: {}", jobId);
        return jobMapper.toDto(savedJob);
    }

    @Transactional
    public JobDTO cancelJob(Long jobId, Long proId, String reason) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Job not found"));

        if (!job.getPro().getId().equals(proId)) {
            throw new com.pro.exceptions.AccessDeniedException("Pro can only cancel their own jobs");
        }

        if (job.getStatus() == JobStatus.DONE) {
            throw new com.pro.exceptions.BadRequestException("Cannot cancel completed job");
        }

        job.setStatus(JobStatus.CANCELLED);
        Job savedJob = jobRepository.save(job);

        // Update request status
        customerRequestService.updateRequestStatus(job.getRequest().getId(), RequestStatus.CANCELLED);

        // Notify client
        notificationService.sendJobCancelledNotification(job, reason);

        log.info("Job cancelled: {}", jobId);
        return jobMapper.toDto(savedJob);
    }

    @Transactional
    public Rating submitRating(RatingSubmissionRequest request, Long clientId) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Job not found"));

        if (!job.getClient().getId().equals(clientId)) {
            throw new com.pro.exceptions.AccessDeniedException("Client can only rate their own jobs");
        }

        if (job.getStatus() != JobStatus.DONE) {
            throw new com.pro.exceptions.BadRequestException("Can only rate completed jobs");
        }

        Rating rating = ratingService.createRating(job, request);

        // Update pro's average rating
        updateProAverageRating(job.getPro().getId());

        return rating;
    }

    private void updateProStats(Long proId) {
        // This would update the pro's completed jobs count
        // Implementation depends on how you want to cache these stats
    }

    private void updateProAverageRating(Long proId) {
        // Recalculate and update pro's average rating
        // This could be done asynchronously for performance
    }
}
