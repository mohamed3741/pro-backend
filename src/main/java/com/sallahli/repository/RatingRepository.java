package com.sallahli.repository;

import com.sallahli.model.Rating;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends GenericRepository<Rating> {

    Optional<Rating> findByJobId(Long jobId);

    List<Rating> findByProIdOrderByCreatedAtDesc(Long proId);

    List<Rating> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.pro.id = :proId")
    Double getAverageRatingByPro(@Param("proId") Long proId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.pro.id = :proId")
    Long countRatingsByPro(@Param("proId") Long proId);

    @Query("SELECT r FROM Rating r WHERE r.pro.id = :proId AND r.stars >= :minStars")
    List<Rating> findPositiveRatingsByPro(@Param("proId") Long proId, @Param("minStars") Integer minStars);
}

