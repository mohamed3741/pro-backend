package com.sallahli.repository;

import com.sallahli.model.Enum.JobStatus;
import com.sallahli.model.Job;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends GenericRepository<Job> {

    List<Job> findByProIdOrderByCreatedAtDesc(Long proId);

    List<Job> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<Job> findByStatus(JobStatus status);

    List<Job> findByProIdAndStatus(Long proId, JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'DONE' AND j.doneAt BETWEEN :startDate AND :endDate")
    List<Job> findCompletedJobsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.pro.id = :proId AND j.status = 'DONE'")
    Long countCompletedJobsByPro(@Param("proId") Long proId);

    @Query("SELECT AVG(r.stars) FROM Job j JOIN Rating r ON j.id = r.job.id WHERE j.pro.id = :proId")
    Double getAverageRatingByPro(@Param("proId") Long proId);
}

