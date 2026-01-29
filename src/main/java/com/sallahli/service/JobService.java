package com.sallahli.service;

import com.sallahli.dto.sallahli.JobDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.JobMapper;
import com.sallahli.model.*;
import com.sallahli.model.Enum.JobStatus;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.repository.*;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class JobService extends AbstractCrudService<Job, JobDTO> {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final CustomerRequestRepository customerRequestRepository;
    private final ProRepository proRepository;
    private final CustomerRequestService customerRequestService;

    public JobService(JobRepository jobRepository,
            JobMapper jobMapper,
            CustomerRequestRepository customerRequestRepository,
            ProRepository proRepository,
            @Lazy CustomerRequestService customerRequestService) {
        super(jobRepository, jobMapper);
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
        this.customerRequestRepository = customerRequestRepository;
        this.proRepository = proRepository;
        this.customerRequestService = customerRequestService;
    }

    // ========================================================================
    // Job Creation
    // ========================================================================

    /**
     * Create a job from an accepted lead offer.
     */
    @Transactional
    public JobDTO createJobFromLeadOffer(LeadOffer leadOffer) {
        CustomerRequest request = leadOffer.getRequest();
        Pro pro = leadOffer.getPro();

        Job job = Job.builder()
                .request(request)
                .leadOffer(leadOffer)
                .pro(pro)
                .client(request.getClient())
                .status(JobStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .archived(false)
                .build();

        Job saved = jobRepository.save(job);

        // Update request status to ASSIGNED
        request.setStatus(RequestStatus.ASSIGNED);
        customerRequestRepository.save(request);

        log.info("Created job {} from lead offer {} for pro {}", saved.getId(), leadOffer.getId(), pro.getId());

        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Job Lifecycle
    // ========================================================================

    /**
     * Complete a job.
     * Updates pro statistics.
     */
    @Transactional
    public JobDTO completeJob(Long jobId) {
        Job job = findJobById(jobId);

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job must be IN_PROGRESS to complete. Current: " + job.getStatus());
        }

        job.setStatus(JobStatus.DONE);
        job.setDoneAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        // Update request status to DONE
        customerRequestService.completeRequest(job.getRequest().getId());

        // Update pro statistics
        updateProStatistics(job.getPro().getId());

        log.info("Completed job {}", jobId);

        return getMapper().toDto(saved);
    }

    /**
     * Cancel a job.
     */
    @Transactional
    public JobDTO cancelJob(Long jobId, String reason) {
        Job job = findJobById(jobId);

        if (job.getStatus() == JobStatus.DONE) {
            throw new BadRequestException("Cannot cancel a completed job");
        }

        job.setStatus(JobStatus.CANCELLED);
        Job saved = jobRepository.save(job);

        log.info("Cancelled job {} with reason: {}", jobId, reason);

        return getMapper().toDto(saved);
    }

    /**
     * Mark job as no-show (pro didn't show up).
     */
    @Transactional
    public JobDTO markNoShow(Long jobId) {
        Job job = findJobById(jobId);

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job must be IN_PROGRESS to mark as no-show. Current: " + job.getStatus());
        }

        job.setStatus(JobStatus.NO_SHOW);
        Job saved = jobRepository.save(job);

        log.info("Marked job {} as no-show", jobId);

        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get jobs for a pro.
     */
    @Transactional(readOnly = true)
    public List<JobDTO> findByProId(Long proId) {
        List<Job> jobs = jobRepository.findByProIdOrderByCreatedAtDesc(proId);
        return getMapper().toDtos(jobs);
    }

    /**
     * Get jobs for a client.
     */
    @Transactional(readOnly = true)
    public List<JobDTO> findByClientId(Long clientId) {
        List<Job> jobs = jobRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return getMapper().toDtos(jobs);
    }

    /**
     * Get jobs by status.
     */
    @Transactional(readOnly = true)
    public List<JobDTO> findByStatus(JobStatus status) {
        List<Job> jobs = jobRepository.findByStatus(status);
        return getMapper().toDtos(jobs);
    }

    /**
     * Get active jobs for a pro (IN_PROGRESS).
     */
    @Transactional(readOnly = true)
    public List<JobDTO> findActiveJobsByProId(Long proId) {
        List<Job> jobs = jobRepository.findByProIdAndStatus(proId, JobStatus.IN_PROGRESS);
        return getMapper().toDtos(jobs);
    }

    /**
     * Get completed jobs in date range.
     */
    @Transactional(readOnly = true)
    public List<JobDTO> findCompletedJobsInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Job> jobs = jobRepository.findCompletedJobsInDateRange(startDate, endDate);
        return getMapper().toDtos(jobs);
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Get job count for a pro.
     */
    @Transactional(readOnly = true)
    public Long countCompletedJobsByPro(Long proId) {
        return jobRepository.countCompletedJobsByPro(proId);
    }

    /**
     * Get average rating for a pro.
     */
    @Transactional(readOnly = true)
    public Double getAverageRatingByPro(Long proId) {
        Double avg = jobRepository.getAverageRatingByPro(proId);
        return avg != null ? avg : 5.0;
    }

    /**
     * Update pro statistics after job completion.
     */
    @Transactional
    protected void updateProStatistics(Long proId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new NotFoundException("Pro not found with id: " + proId));

        Long completedJobs = jobRepository.countCompletedJobsByPro(proId);
        Double avgRating = jobRepository.getAverageRatingByPro(proId);

        pro.setJobsCompleted(completedJobs != null ? completedJobs : 0L);
        if (avgRating != null) {
            pro.setRatingAvg(avgRating);
        }

        proRepository.save(pro);
        log.debug("Updated pro {} statistics: jobs={}, rating={}", proId, completedJobs, avgRating);
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Job findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found with id: " + id));
    }
}
