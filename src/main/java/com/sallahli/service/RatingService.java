package com.sallahli.service;

import com.sallahli.dto.sallahli.request.RatingSubmissionRequest;
import com.sallahli.model.Job;
import com.sallahli.model.Rating;
import com.sallahli.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;

    @Transactional
    public Rating createRating(Job job, RatingSubmissionRequest request) {
        // Check if rating already exists
        if (ratingRepository.findByJobId(job.getId()).isPresent()) {
            throw new com.sallahli.exceptions.ConflictAccountException("Rating already exists for this job");
        }

        Rating rating = Rating.builder()
                .job(job)
                .request(job.getRequest())
                .client(job.getClient())
                .pro(job.getPro())
                .stars(request.getStars())
                .comment(request.getComment())
                .build();

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating submitted for job {}: {} stars", job.getId(), request.getStars());

        return savedRating;
    }

    @Transactional(readOnly = true)
    public Double getProAverageRating(Long proId) {
        return ratingRepository.getAverageRatingByPro(proId);
    }

    @Transactional(readOnly = true)
    public Long getProRatingCount(Long proId) {
        return ratingRepository.countRatingsByPro(proId);
    }
}

